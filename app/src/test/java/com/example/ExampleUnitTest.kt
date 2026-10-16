package com.example

import com.example.pskprop.model.HamBand
import com.example.pskprop.util.GeoMath
import com.example.pskprop.util.MaidenheadUtil
import com.example.pskprop.util.SolarTerminator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testMaidenheadConversion() {
        // KP20 is in southern Finland (Helsinki area, ~60.5° N, ~25.0° E)
        val coords = MaidenheadUtil.maidenheadToLatLon("KP20")
        assertNotNull(coords)
        assertEquals(60.5, coords!!.first, 0.6)
        assertEquals(25.0, coords.second, 1.1)

        // Reverse conversion
        val locator = MaidenheadUtil.latLonToMaidenhead(coords.first, coords.second, length = 4)
        assertEquals("KP20", locator)
    }

    @Test
    fun testHaversineDistanceAndBearing() {
        // Helsinki (KP20: ~60.17° N, 24.94° E) to Tokyo (PM95: ~35.68° N, 139.69° E)
        val dist = GeoMath.haversineKm(60.17, 24.94, 35.68, 139.69)
        // Distance should be approximately 7800 km
        assertTrue("Distance $dist should be around 7800 km", dist in 7500.0..8200.0)

        val bearing = GeoMath.initialBearing(60.17, 24.94, 35.68, 139.69)
        // Initial bearing from Helsinki to Tokyo is North-East (~45-55°)
        assertTrue("Bearing $bearing should be around 50°", bearing in 40.0..60.0)
    }

    @Test
    fun testHamBandFromFrequency() {
        assertEquals("20m", HamBand.fromFrequency(14_074_000L)?.label)
        assertEquals("40m", HamBand.fromFrequency(7_074_000L)?.label)
        assertEquals("15m", HamBand.fromFrequency(21_074_000L)?.label)
        assertEquals("10m", HamBand.fromFrequency(28_074_000L)?.label)
        assertEquals("80m", HamBand.fromFrequency(3_573_000L)?.label)
        assertEquals("6m", HamBand.fromFrequency(50_313_000L)?.label)
    }

    @Test
    fun testSolarTerminatorCalculation() {
        val coords = SolarTerminator.computeSolarCoordinates(System.currentTimeMillis())
        assertTrue(coords.subsolarLat in -24.0..24.0)
        assertTrue(coords.subsolarLon in -180.0..180.0)
    }

    @Test
    fun testDotScaling() {
        val spot = com.example.pskprop.model.Spot(
            id = "test",
            callsign = "OH2XX",
            locator = "KP20",
            lat = 60.17,
            lon = 24.94,
            partnerCallsign = "W1AW",
            partnerLocator = "FN31",
            partnerLat = 41.7,
            partnerLon = -72.7,
            band = "20m",
            frequencyHz = 14074000L,
            snr = -15,
            timestampSeconds = 1700000000L,
            mode = "FT8",
            kind = com.example.pskprop.model.SpotRole.SENDER
        )
        // At mid SNR (-15), base is 4.5f. Scaled by 0.50f = 2.25f
        assertEquals(2.25f, spot.getDotRadiusDp(0.50f), 0.01f)
        // Scaled by 1.0f = 4.5f
        assertEquals(4.5f, spot.getDotRadiusDp(1.0f), 0.01f)
    }

    @Test
    fun testSpotAgePruning() {
        val nowSec = System.currentTimeMillis() / 1000L
        val freshSpot = com.example.pskprop.model.Spot(
            id = "fresh",
            callsign = "W1AW",
            locator = "FN31",
            lat = 41.7,
            lon = -72.7,
            partnerCallsign = "OH2XX",
            partnerLocator = "KP20",
            partnerLat = 60.17,
            partnerLon = 24.94,
            band = "20m",
            frequencyHz = 14074000L,
            snr = -10,
            timestampSeconds = nowSec - 300L, // 5 minutes old
            mode = "FT8",
            kind = com.example.pskprop.model.SpotRole.SENDER
        )
        val oldSpot = com.example.pskprop.model.Spot(
            id = "old",
            callsign = "JA1ZLO",
            locator = "PM95",
            lat = 35.6,
            lon = 139.7,
            partnerCallsign = "OH2XX",
            partnerLocator = "KP20",
            partnerLat = 60.17,
            partnerLon = 24.94,
            band = "20m",
            frequencyHz = 14074000L,
            snr = -18,
            timestampSeconds = nowSec - 1800L, // 30 minutes old
            mode = "FT8",
            kind = com.example.pskprop.model.SpotRole.SENDER
        )

        val ageMinutes = 15
        val cutoffSec = nowSec - (ageMinutes * 60L)
        val spots = listOf(freshSpot, oldSpot)
        val retained = spots.filter { it.timestampSeconds >= cutoffSec }

        assertEquals(1, retained.size)
        assertEquals("fresh", retained[0].id)
    }
}
