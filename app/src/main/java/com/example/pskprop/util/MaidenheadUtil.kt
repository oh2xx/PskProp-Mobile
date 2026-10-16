package com.example.pskprop.util

import kotlin.math.floor

object MaidenheadUtil {

    /**
     * Converts a Maidenhead locator (2, 4, 6, or 8 characters) to latitude and longitude (center of grid).
     */
    fun maidenheadToLatLon(grid: String?): Pair<Double, Double>? {
        if (grid.isNullOrBlank() || grid.length < 2) return null
        var g = grid.trim().uppercase()
        if (g.length % 2 != 0) {
            g += "MM"
        }

        try {
            // Field (first pair: A-R)
            val f0 = g[0]
            val f1 = g[1]
            if (f0 !in 'A'..'R' || f1 !in 'A'..'R') return null

            var lon = (f0 - 'A') * 20.0 - 180.0
            var lat = (f1 - 'A') * 10.0 - 90.0

            var sizeLon = 20.0
            var sizeLat = 10.0

            // Square (second pair: 0-9)
            if (g.length >= 4) {
                val s0 = g[2]
                val s1 = g[3]
                if (s0 !in '0'..'9' || s1 !in '0'..'9') return null
                lon += (s0 - '0') * 2.0
                lat += (s1 - '0') * 1.0
                sizeLon = 2.0
                sizeLat = 1.0
            }

            // Subsquare (third pair: A-X)
            if (g.length >= 6) {
                val sub0 = g[4]
                val sub1 = g[5]
                if (sub0 !in 'A'..'X' || sub1 !in 'A'..'X') return null
                lon += (sub0 - 'A') * (5.0 / 60.0)
                lat += (sub1 - 'A') * (2.5 / 60.0)
                sizeLon = 5.0 / 60.0
                sizeLat = 2.5 / 60.0
            }

            // Extended square (fourth pair: 0-9)
            if (g.length >= 8) {
                val e0 = g[6]
                val e1 = g[7]
                if (e0 !in '0'..'9' || e1 !in '0'..'9') return null
                lon += (e0 - '0') * (0.5 / 60.0)
                lat += (e1 - '0') * (0.25 / 60.0)
                sizeLon = 0.5 / 60.0
                sizeLat = 0.25 / 60.0
            }

            // Return center of grid
            lon += sizeLon / 2.0
            lat += sizeLat / 2.0

            return Pair(lat, lon)
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Converts a lat/lon to Maidenhead locator string (default 6 characters, e.g. KP20mb).
     */
    fun latLonToMaidenhead(lat: Double, lon: Double, length: Int = 6): String {
        var adjLat = lat + 90.0
        var adjLon = lon + 180.0

        adjLat = adjLat.coerceIn(0.0, 179.999999)
        adjLon = adjLon.coerceIn(0.0, 359.999999)

        val fieldLon = floor(adjLon / 20.0).toInt()
        val fieldLat = floor(adjLat / 10.0).toInt()

        val sb = StringBuilder()
        sb.append(('A'.code + fieldLon).toChar())
        sb.append(('A'.code + fieldLat).toChar())

        if (length >= 4) {
            val remLon4 = adjLon - fieldLon * 20.0
            val remLat4 = adjLat - fieldLat * 10.0
            val sqLon = floor(remLon4 / 2.0).toInt()
            val sqLat = floor(remLat4 / 1.0).toInt()
            sb.append(('0'.code + sqLon).toChar())
            sb.append(('0'.code + sqLat).toChar())

            if (length >= 6) {
                val remLon6 = remLon4 - sqLon * 2.0
                val remLat6 = remLat4 - sqLat * 1.0
                val subLon = floor(remLon6 / (5.0 / 60.0)).toInt()
                val subLat = floor(remLat6 / (2.5 / 60.0)).toInt()
                sb.append(('a'.code + subLon).toChar())
                sb.append(('a'.code + subLat).toChar())
            }
        }

        return sb.toString()
    }

    fun isValidLocator(grid: String?): Boolean {
        if (grid.isNullOrBlank()) return false
        val clean = grid.trim().uppercase()
        if (clean.length !in listOf(2, 4, 6, 8)) return false
        return maidenheadToLatLon(clean) != null
    }
}
