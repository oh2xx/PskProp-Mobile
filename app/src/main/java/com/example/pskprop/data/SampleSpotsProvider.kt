package com.example.pskprop.data

import com.example.pskprop.model.HamBand
import com.example.pskprop.model.Spot
import com.example.pskprop.model.SpotRole
import com.example.pskprop.util.GeoMath
import com.example.pskprop.util.MaidenheadUtil

object SampleSpotsProvider {

    fun generateSampleSpots(homeLat: Double, homeLon: Double): List<Spot> {
        val nowSec = System.currentTimeMillis() / 1000L

        val raw = listOf(
            // Sender spots (remote transmitters heard around Scandinavia / Europe)
            Triple("W1AW", "FN31pr", -14 to 14074000L),
            Triple("JA1ZLO", "PM95sq", -18 to 14075100L),
            Triple("PY2XB", "GG66pt", -22 to 21074000L),
            Triple("VK2GR", "QF56oe", -19 to 14074500L),
            Triple("G4BAO", "JO02ac", -6 to 7074000L),
            Triple("DL1ABC", "JO42ge", -3 to 7074800L),
            Triple("SM0XYZ", "JO89xp", 4 to 14074200L),
            Triple("K3LR", "EN91ve", -11 to 28074000L),
            Triple("ZS6BTL", "KG44de", -17 to 21075200L),
            Triple("EA3JE", "JN11bi", -8 to 14075800L),
            Triple("F6KNB", "IN94ur", -10 to 10136000L),
            Triple("LA7W", "JP53eg", 2 to 3573000L),
            Triple("OH2B", "KP20le", 12 to 14074900L),
            Triple("UA9FVR", "LO88sa", -13 to 14074300L),
            Triple("VE3DZ", "FN03eu", -16 to 18100000L),
            Triple("ZL1ANH", "RF73lb", -21 to 14074000L),
            Triple("CU3AA", "HM68kp", -15 to 24915000L),
            Triple("SV8/DJ6SI", "KM17tu", -7 to 28074500L),
            Triple("OH1F", "KP11ap", 8 to 50313000L),
            Triple("EI7M", "IO51tu", -9 to 14075200L)
        )

        val partnerCall = "OH2XX"
        val partnerLoc = "KP20"
        val partnerCoords = MaidenheadUtil.maidenheadToLatLon(partnerLoc)

        return raw.mapIndexed { idx, (call, grid, meta) ->
            val (snr, freqHz) = meta
            val coords = MaidenheadUtil.maidenheadToLatLon(grid) ?: Pair(50.0, 10.0)
            val band = HamBand.fromFrequency(freqHz)?.label ?: "20m"
            val dist = GeoMath.haversineKm(homeLat, homeLon, coords.first, coords.second)
            val bearing = GeoMath.initialBearing(homeLat, homeLon, coords.first, coords.second)
            val role = if (idx % 3 == 0) SpotRole.RECEIVER else SpotRole.SENDER

            Spot(
                id = "sample_$idx",
                callsign = call,
                locator = grid,
                lat = coords.first,
                lon = coords.second,
                partnerCallsign = partnerCall,
                partnerLocator = partnerLoc,
                partnerLat = partnerCoords?.first,
                partnerLon = partnerCoords?.second,
                band = band,
                frequencyHz = freqHz,
                snr = snr,
                timestampSeconds = nowSec - (idx * 45),
                mode = "FT8",
                kind = role,
                dxcc = when {
                    call.startsWith("W") || call.startsWith("K") -> "United States"
                    call.startsWith("JA") -> "Japan"
                    call.startsWith("PY") -> "Brazil"
                    call.startsWith("VK") -> "Australia"
                    call.startsWith("G") -> "England"
                    call.startsWith("DL") -> "Fed. Rep. of Germany"
                    call.startsWith("SM") -> "Sweden"
                    call.startsWith("ZS") -> "South Africa"
                    call.startsWith("EA") -> "Spain"
                    call.startsWith("F") -> "France"
                    call.startsWith("LA") -> "Norway"
                    call.startsWith("OH") -> "Finland"
                    call.startsWith("UA") -> "Asiatic Russia"
                    call.startsWith("VE") -> "Canada"
                    call.startsWith("ZL") -> "New Zealand"
                    call.startsWith("CU") -> "Azores"
                    call.startsWith("SV") -> "Greece"
                    call.startsWith("EI") -> "Ireland"
                    else -> "Unknown"
                },
                distanceKm = dist,
                bearingDeg = bearing
            )
        }
    }
}
