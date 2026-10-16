package com.example.pskprop.ui.map

import androidx.compose.ui.geometry.Offset
import com.example.pskprop.model.MapProjectionType
import com.example.pskprop.util.GeoMath
import kotlin.math.*

object MapProjectionEngine {

    // Robinson lookup table (lat in 5-degree steps from 0 to 90)
    // Pair(X_factor, Y_ratio)
    private val ROBINSON_TABLE = arrayOf(
        1.0000 to 0.0000, // 0°
        0.9986 to 0.0620, // 5°
        0.9954 to 0.1240, // 10°
        0.9900 to 0.1860, // 15°
        0.9822 to 0.2480, // 20°
        0.9730 to 0.3100, // 25°
        0.9600 to 0.3720, // 30°
        0.9427 to 0.4340, // 35°
        0.9216 to 0.4958, // 40°
        0.8962 to 0.5571, // 45°
        0.8679 to 0.6176, // 50°
        0.8350 to 0.6769, // 55°
        0.7986 to 0.7346, // 60°
        0.7597 to 0.7903, // 65°
        0.7186 to 0.8435, // 70°
        0.6732 to 0.8936, // 75°
        0.6213 to 0.9394, // 80°
        0.5722 to 0.9761, // 85°
        0.5322 to 1.0000  // 90°
    )

    fun project(
        lat: Double,
        lon: Double,
        type: MapProjectionType,
        homeLat: Double,
        homeLon: Double,
        width: Float,
        height: Float,
        zoom: Float,
        pan: Offset
    ): Offset {
        val centerX = width / 2f + pan.x
        val centerY = height / 2f + pan.y

        return when (type) {
            MapProjectionType.AEQD -> projectAeqd(lat, lon, homeLat, homeLon, centerX, centerY, width, height, zoom)
            MapProjectionType.ROBINSON -> projectRobinson(lat, lon, homeLon, centerX, centerY, width, height, zoom)
            MapProjectionType.MERCATOR -> projectMercator(lat, lon, homeLon, centerX, centerY, width, height, zoom)
        }
    }

    private fun projectAeqd(
        lat: Double,
        lon: Double,
        homeLat: Double,
        homeLon: Double,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
        zoom: Float
    ): Offset {
        val maxDistKm = Math.PI * GeoMath.EARTH_RADIUS_KM // ~20015 km
        val distKm = GeoMath.haversineKm(homeLat, homeLon, lat, lon)
        val bearingDeg = GeoMath.initialBearing(homeLat, homeLon, lat, lon)

        val mapRadius = (min(width, height) / 2f - 24f) * zoom
        val r = (distKm / maxDistKm).toFloat() * mapRadius
        val rad = Math.toRadians(bearingDeg)

        val x = cx + r * sin(rad).toFloat()
        val y = cy - r * cos(rad).toFloat()
        return Offset(x, y)
    }

    private fun projectRobinson(
        lat: Double,
        lon: Double,
        homeLon: Double,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
        zoom: Float
    ): Offset {
        var dLon = lon - homeLon
        while (dLon > 180.0) dLon -= 360.0
        while (dLon < -180.0) dLon += 360.0

        val absLat = abs(lat).coerceIn(0.0, 90.0)
        val signLat = if (lat < 0) -1f else 1f

        val index = (absLat / 5.0).toInt().coerceIn(0, 17)
        val frac = ((absLat % 5.0) / 5.0).toFloat()

        val p0 = ROBINSON_TABLE[index]
        val p1 = ROBINSON_TABLE[index + 1]

        val xFactor = (p0.first + (p1.first - p0.first) * frac).toFloat()
        val yRatio = (p0.second + (p1.second - p0.second) * frac).toFloat()

        val baseW = (width - 32f) * 0.48f * zoom
        val baseH = (height - 32f) * 0.45f * zoom

        val x = cx + (dLon / 180.0).toFloat() * xFactor * baseW
        val y = cy - signLat * yRatio * baseH
        return Offset(x, y)
    }

    private fun projectMercator(
        lat: Double,
        lon: Double,
        homeLon: Double,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
        zoom: Float
    ): Offset {
        var dLon = lon - homeLon
        while (dLon > 180.0) dLon -= 360.0
        while (dLon < -180.0) dLon += 360.0

        val clampedLat = lat.coerceIn(-82.0, 82.0)
        val latRad = Math.toRadians(clampedLat)
        val mercY = ln(tan(Math.PI / 4.0 + latRad / 2.0)) / Math.PI

        val baseW = (width - 32f) * 0.48f * zoom
        val baseH = (height - 32f) * 0.42f * zoom

        val x = cx + (dLon / 180.0).toFloat() * baseW
        val y = cy - (mercY.toFloat() * baseH)
        return Offset(x, y)
    }
}
