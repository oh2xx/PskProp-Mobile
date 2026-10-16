package com.example.pskprop.util

import kotlin.math.*

object GeoMath {
    const val EARTH_RADIUS_KM = 6371.0088
    const val KM_TO_MILES = 0.621371192

    /**
     * Haversine formula for Great Circle distance in kilometers.
     */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2.0).pow(2.0) +
                cos(rLat1) * cos(rLat2) * sin(dLon / 2.0).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(max(0.0, 1.0 - a)))
        return EARTH_RADIUS_KM * c
    }

    fun haversineMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return haversineKm(lat1, lon1, lat2, lon2) * KM_TO_MILES
    }

    /**
     * Initial bearing (forward azimuth) from point 1 to point 2 in degrees [0, 360).
     */
    fun initialBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)

        val y = sin(dLon) * cos(rLat2)
        val x = cos(rLat1) * sin(rLat2) - sin(rLat1) * cos(rLat2) * cos(dLon)
        val rad = atan2(y, x)
        val deg = Math.toDegrees(rad)
        return (deg + 360.0) % 360.0
    }

    /**
     * Back bearing from point 2 to point 1.
     */
    fun backBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return (initialBearing(lat2, lon2, lat1, lon1) + 360.0) % 360.0
    }

    /**
     * Human-readable 16-point compass direction (e.g. N, NNE, NE...).
     */
    fun compassHeading(bearingDeg: Double): String {
        val directions = arrayOf(
            "N", "NNE", "NE", "ENE",
            "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW",
            "W", "WNW", "NW", "NNW"
        )
        val normalized = (bearingDeg % 360.0 + 360.0) % 360.0
        val index = floor((normalized + 11.25) / 22.5).toInt() % 16
        return directions[index]
    }

    /**
     * Generates intermediate points along great circle arc for visualization.
     */
    fun greatCircleArc(lat1: Double, lon1: Double, lat2: Double, lon2: Double, numSegments: Int = 30): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        val rLat1 = Math.toRadians(lat1)
        val rLon1 = Math.toRadians(lon1)
        val rLat2 = Math.toRadians(lat2)
        val rLon2 = Math.toRadians(lon2)

        val d = 2.0 * asin(sqrt(sin((rLat1 - rLat2) / 2.0).pow(2.0) +
                cos(rLat1) * cos(rLat2) * sin((rLon1 - rLon2) / 2.0).pow(2.0)))

        if (d < 1e-6) {
            return listOf(Pair(lat1, lon1), Pair(lat2, lon2))
        }

        for (i in 0..numSegments) {
            val f = i.toDouble() / numSegments.toDouble()
            val a = sin((1 - f) * d) / sin(d)
            val b = sin(f * d) / sin(d)
            val x = a * cos(rLat1) * cos(rLon1) + b * cos(rLat2) * cos(rLon2)
            val y = a * cos(rLat1) * sin(rLon1) + b * cos(rLat2) * sin(rLon2)
            val z = a * sin(rLat1) + b * sin(rLat2)
            val lat = atan2(z, sqrt(x * x + y * y))
            val lon = atan2(y, x)
            points.add(Pair(Math.toDegrees(lat), Math.toDegrees(lon)))
        }
        return points
    }
}
