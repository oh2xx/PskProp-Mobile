package com.example.pskprop.util

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.*

object SolarTerminator {
    private const val RAD = Math.PI / 180.0
    private const val DEG = 180.0 / Math.PI

    data class SolarCoords(
        val subsolarLon: Double,
        val subsolarLat: Double,
        val antipodalLon: Double,
        val antipodalLat: Double
    )

    /**
     * Computes the NOAA subsolar point (lat, lon) for any given timestamp.
     */
    fun computeSolarCoordinates(timeMs: Long = System.currentTimeMillis()): SolarCoords {
        val date = Date(timeMs)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = date }

        val centuries = (timeMs - 946728000000L) / 86400000.0 / 36525.0 // centuries since J2000

        // Start of current UTC day
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis

        val longitude = (dayStart - timeMs) / 86400000.0 * 360.0 - 180.0

        val eqTime = equationOfTime(centuries)
        val decl = solarDeclination(centuries)

        val subsolarLon = (longitude - eqTime * DEG).let {
            var l = it % 360.0
            if (l > 180.0) l -= 360.0
            if (l < -180.0) l += 360.0
            l
        }
        val subsolarLat = decl * DEG

        var antiLon = subsolarLon + 180.0
        if (antiLon > 180.0) antiLon -= 360.0
        val antiLat = -subsolarLat

        return SolarCoords(
            subsolarLon = subsolarLon,
            subsolarLat = subsolarLat,
            antipodalLon = antiLon,
            antipodalLat = antiLat
        )
    }

    /**
     * Computes solar elevation angle above horizon in degrees for any location.
     * > 0: Day
     * 0 to -6: Civil twilight (Grayline)
     * -6 to -12: Nautical twilight (Grayline)
     * < -12: Night
     */
    fun getSolarAltitude(lat: Double, lon: Double, coords: SolarCoords): Double {
        val phi = lat * RAD
        val delta = coords.subsolarLat * RAD
        val h = (lon - coords.subsolarLon) * RAD

        val sinAlt = sin(phi) * sin(delta) + cos(phi) * cos(delta) * cos(h)
        return asin(sinAlt.coerceIn(-1.0, 1.0)) * DEG
    }

    private fun equationOfTime(t: Double): Double {
        val e = eccentricityEarthOrbit(t)
        val m = solarGeometricMeanAnomaly(t)
        val l = solarGeometricMeanLongitude(t)
        val y = tan(obliquityCorrection(t) / 2.0)
        val y2 = y * y
        return y2 * sin(2.0 * l) -
                2.0 * e * sin(m) +
                4.0 * e * y2 * sin(m) * cos(2.0 * l) -
                0.5 * y2 * y2 * sin(4.0 * l) -
                1.25 * e * e * sin(2.0 * m)
    }

    private fun solarDeclination(t: Double): Double {
        return asin(sin(obliquityCorrection(t)) * sin(solarApparentLongitude(t)))
    }

    private fun solarApparentLongitude(t: Double): Double {
        return solarTrueLongitude(t) - (0.00569 + 0.00478 * sin((125.04 - 1934.136 * t) * RAD)) * RAD
    }

    private fun solarTrueLongitude(t: Double): Double {
        return solarGeometricMeanLongitude(t) + solarEquationOfCenter(t)
    }

    private fun solarGeometricMeanAnomaly(t: Double): Double {
        return (357.52911 + t * (35999.05029 - 0.0001537 * t)) * RAD
    }

    private fun solarGeometricMeanLongitude(t: Double): Double {
        var l = (280.46646 + t * (36000.76983 + t * 0.0003032)) % 360.0
        if (l < 0) l += 360.0
        return l * RAD
    }

    private fun solarEquationOfCenter(t: Double): Double {
        val m = solarGeometricMeanAnomaly(t)
        return (sin(m) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
                sin(2.0 * m) * (0.019993 - 0.000101 * t) +
                sin(3.0 * m) * 0.000289) * RAD
    }

    private fun obliquityCorrection(t: Double): Double {
        val eps0 = meanObliquityOfEcliptic(t)
        return eps0 + 0.00256 * cos((125.04 - 1934.136 * t) * RAD) * RAD
    }

    private fun meanObliquityOfEcliptic(t: Double): Double {
        return (23.0 + (26.0 + (21.448 - t * (46.8150 + t * (0.00059 - 0.001813 * t))) / 60.0) / 60.0) * RAD
    }

    private fun eccentricityEarthOrbit(t: Double): Double {
        return 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
    }
}
