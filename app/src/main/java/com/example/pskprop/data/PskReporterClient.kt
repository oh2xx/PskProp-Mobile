package com.example.pskprop.data

import android.util.Xml
import com.example.pskprop.model.HamBand
import com.example.pskprop.model.PropConfig
import com.example.pskprop.model.QueryDirection
import com.example.pskprop.model.Spot
import com.example.pskprop.model.SpotRole
import com.example.pskprop.util.GeoMath
import com.example.pskprop.util.MaidenheadUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.concurrent.TimeUnit

class PskReporterClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    suspend fun querySpots(config: PropConfig): Result<List<Spot>> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = HttpUrl.Builder()
                .scheme("https")
                .host("retrieve.pskreporter.info")
                .addPathSegment("query")

            val ageSec = -(config.ageMinutes * 60)
            urlBuilder.addQueryParameter("age", ageSec.toString())
            urlBuilder.addQueryParameter("rronly", "1")
            urlBuilder.addQueryParameter("modify", "grid")

            val call = config.callsignFilter.trim().uppercase()
            if (call.isNotEmpty()) {
                when (config.queryDirection) {
                    QueryDirection.BOTH -> urlBuilder.addQueryParameter("callsign", call)
                    QueryDirection.TX_ONLY -> urlBuilder.addQueryParameter("senderCallsign", call)
                    QueryDirection.RX_ONLY -> urlBuilder.addQueryParameter("receiverCallsign", call)
                }
            } else {
                // If no specific callsign, query by band or grid
                if (config.selectedBands.size == 1) {
                    urlBuilder.addQueryParameter("band", config.selectedBands.first())
                } else if (config.homeLocator.isNotBlank()) {
                    val gridPrefix = config.homeLocator.trim().take(4)
                    urlBuilder.addQueryParameter("grid", gridPrefix)
                }
            }

            if (config.selectedMode != "All" && config.selectedMode.isNotBlank()) {
                urlBuilder.addQueryParameter("mode", config.selectedMode)
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", "PSKProp-Android/1.0 (Amateur Radio Propagation Monitor)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
            }

            val xmlBody = response.body?.string().orEmpty()
            if (xmlBody.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val spots = parseXmlReports(xmlBody, config)
            Result.success(spots)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseXmlReports(xml: String, config: PropConfig): List<Spot> {
        val spots = mutableListOf<Spot>()
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var spotIndex = 0

        val homeLat = config.homeLat
        val homeLon = config.homeLon
        val radiusKm = config.radiusKm
        val queryCall = config.callsignFilter.trim().uppercase()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "receptionReport") {
                val senderCall = parser.getAttributeValue(null, "senderCallsign").orEmpty()
                val senderGrid = parser.getAttributeValue(null, "senderLocator").orEmpty()
                val receiverCall = parser.getAttributeValue(null, "receiverCallsign").orEmpty()
                val receiverGrid = parser.getAttributeValue(null, "receiverLocator").orEmpty()
                val freqStr = parser.getAttributeValue(null, "frequency")
                val freqHz = freqStr?.toLongOrNull()
                val mode = parser.getAttributeValue(null, "mode") ?: "FT8"
                val snrStr = parser.getAttributeValue(null, "sNR")
                val snr = snrStr?.toIntOrNull()
                val timeStr = parser.getAttributeValue(null, "flowStartSeconds")
                val timestamp = timeStr?.toDoubleOrNull()?.toLong() ?: (System.currentTimeMillis() / 1000L)
                val senderDXCC = parser.getAttributeValue(null, "senderDXCC")

                val senderCoords = MaidenheadUtil.maidenheadToLatLon(senderGrid)
                val receiverCoords = MaidenheadUtil.maidenheadToLatLon(receiverGrid)

                // Need at least one valid coordinate set
                if (senderCoords != null || receiverCoords != null) {
                    val bandObj = HamBand.fromFrequency(freqHz)
                    val bandLabel = bandObj?.label ?: "Unknown"

                    // Band filtering
                    val bandAllowed = config.selectedBands.isEmpty() || config.selectedBands.contains(bandLabel)
                    val modeAllowed = config.selectedMode == "All" || config.selectedMode.equals(mode, ignoreCase = true)

                    if (bandAllowed && modeAllowed) {
                        val sl = senderCoords
                        val rl = receiverCoords

                        // Radius determination matching pskprop
                        var chosenSpot: Spot? = null

                        if (sl != null && rl != null) {
                            val ds = GeoMath.haversineKm(homeLat, homeLon, sl.first, sl.second)
                            val dr = GeoMath.haversineKm(homeLat, homeLon, rl.first, rl.second)

                            val spotId = "${senderCall}_${receiverCall}_${freqHz ?: bandLabel}_${timestamp}"
                            if (dr <= radiusKm) {
                                // Receiver within radius -> plot sender
                                chosenSpot = Spot(
                                    id = spotId,
                                    callsign = senderCall,
                                    locator = senderGrid,
                                    lat = sl.first,
                                    lon = sl.second,
                                    partnerCallsign = receiverCall,
                                    partnerLocator = receiverGrid,
                                    partnerLat = rl.first,
                                    partnerLon = rl.second,
                                    band = bandLabel,
                                    frequencyHz = freqHz,
                                    snr = snr,
                                    timestampSeconds = timestamp,
                                    mode = mode,
                                    kind = SpotRole.SENDER,
                                    dxcc = senderDXCC,
                                    distanceKm = ds,
                                    bearingDeg = GeoMath.initialBearing(homeLat, homeLon, sl.first, sl.second)
                                )
                            } else if (ds <= radiusKm) {
                                // Sender within radius -> plot receiver
                                chosenSpot = Spot(
                                    id = spotId,
                                    callsign = receiverCall,
                                    locator = receiverGrid,
                                    lat = rl.first,
                                    lon = rl.second,
                                    partnerCallsign = senderCall,
                                    partnerLocator = senderGrid,
                                    partnerLat = sl.first,
                                    partnerLon = sl.second,
                                    band = bandLabel,
                                    frequencyHz = freqHz,
                                    snr = snr,
                                    timestampSeconds = timestamp,
                                    mode = mode,
                                    kind = SpotRole.RECEIVER,
                                    dxcc = senderDXCC,
                                    distanceKm = dr,
                                    bearingDeg = GeoMath.initialBearing(homeLat, homeLon, rl.first, rl.second)
                                )
                            } else if (radiusKm >= 19000.0 || queryCall.isNotEmpty()) {
                                // Global or specific callsign query mode
                                val isSenderTarget = queryCall.isNotEmpty() && senderCall.equals(queryCall, ignoreCase = true)
                                if (isSenderTarget) {
                                    chosenSpot = Spot(
                                        id = spotId,
                                        callsign = receiverCall,
                                        locator = receiverGrid,
                                        lat = rl.first,
                                        lon = rl.second,
                                        partnerCallsign = senderCall,
                                        partnerLocator = senderGrid,
                                        partnerLat = sl.first,
                                        partnerLon = sl.second,
                                        band = bandLabel,
                                        frequencyHz = freqHz,
                                        snr = snr,
                                        timestampSeconds = timestamp,
                                        mode = mode,
                                        kind = SpotRole.RECEIVER,
                                        dxcc = senderDXCC,
                                        distanceKm = dr,
                                        bearingDeg = GeoMath.initialBearing(homeLat, homeLon, rl.first, rl.second)
                                    )
                                } else {
                                    chosenSpot = Spot(
                                        id = spotId,
                                        callsign = senderCall,
                                        locator = senderGrid,
                                        lat = sl.first,
                                        lon = sl.second,
                                        partnerCallsign = receiverCall,
                                        partnerLocator = receiverGrid,
                                        partnerLat = rl.first,
                                        partnerLon = rl.second,
                                        band = bandLabel,
                                        frequencyHz = freqHz,
                                        snr = snr,
                                        timestampSeconds = timestamp,
                                        mode = mode,
                                        kind = SpotRole.SENDER,
                                        dxcc = senderDXCC,
                                        distanceKm = ds,
                                        bearingDeg = GeoMath.initialBearing(homeLat, homeLon, sl.first, sl.second)
                                    )
                                }
                            }
                        } else if (sl != null) {
                            val ds = GeoMath.haversineKm(homeLat, homeLon, sl.first, sl.second)
                            val spotId = "${senderCall}_${receiverCall}_${freqHz ?: bandLabel}_${timestamp}"
                            if (radiusKm >= 19000.0 || ds <= radiusKm || queryCall.isNotEmpty()) {
                                chosenSpot = Spot(
                                    id = spotId,
                                    callsign = senderCall,
                                    locator = senderGrid,
                                    lat = sl.first,
                                    lon = sl.second,
                                    partnerCallsign = receiverCall,
                                    partnerLocator = receiverGrid,
                                    partnerLat = null,
                                    partnerLon = null,
                                    band = bandLabel,
                                    frequencyHz = freqHz,
                                    snr = snr,
                                    timestampSeconds = timestamp,
                                    mode = mode,
                                    kind = SpotRole.SENDER,
                                    dxcc = senderDXCC,
                                    distanceKm = ds,
                                    bearingDeg = GeoMath.initialBearing(homeLat, homeLon, sl.first, sl.second)
                                )
                            }
                        }

                        if (chosenSpot != null) {
                            spots.add(chosenSpot)
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return spots
    }
}
