package com.example.pskprop.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pskprop.model.HamBand
import com.example.pskprop.model.MapProjectionType
import com.example.pskprop.model.PropConfig
import com.example.pskprop.model.Spot
import com.example.pskprop.util.GeoMath
import com.example.pskprop.util.SolarTerminator
import com.example.pskprop.util.WorldMapData
import kotlin.math.*

@Composable
fun PropagationMap(
    config: PropConfig,
    spots: List<Spot>,
    selectedSpot: Spot?,
    onSpotSelected: (Spot?) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoom by remember(config.mapProjection) { mutableFloatStateOf(1f) }
    var pan by remember(config.mapProjection) { mutableStateOf(Offset.Zero) }

    val textMeasurer = rememberTextMeasurer()

    // Store projected dot screen positions for hit-testing
    var projectedSpots by remember { mutableStateOf<List<Pair<Spot, Offset>>>(emptyList()) }

    val oceanColor = if (config.darkMapTheme) Color(0xFF0D172A) else Color(0xFFE2E8F0)
    val landFillColor = if (config.darkMapTheme) Color(0xFF1E293B) else Color(0xFFCBD5E1)
    val landStrokeColor = if (config.darkMapTheme) Color(0xFF334155) else Color(0xFF94A3B8)
    val ringColor = if (config.darkMapTheme) Color(0x5538BDF8) else Color(0x550284C7)
    val graylineColor = Color(0x40020617)
    val terminatorLineColor = Color(0xAAFFB703)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(oceanColor)
            .pointerInput(spots, projectedSpots) {
                detectTapGestures(
                    onDoubleTap = {
                        zoom = 1f
                        pan = Offset.Zero
                    },
                    onTap = { tapOffset ->
                        val hitRadiusSq = 36f * 36f
                        val nearest = projectedSpots.minByOrNull { (_, offset) ->
                            val dx = offset.x - tapOffset.x
                            val dy = offset.y - tapOffset.y
                            dx * dx + dy * dy
                        }

                        if (nearest != null) {
                            val dx = nearest.second.x - tapOffset.x
                            val dy = nearest.second.y - tapOffset.y
                            if (dx * dx + dy * dy <= hitRadiusSq) {
                                onSpotSelected(if (selectedSpot?.id == nearest.first.id) null else nearest.first)
                                return@detectTapGestures
                            }
                        }
                        onSpotSelected(null)
                    }
                )
            }
            .pointerInput(config.mapProjection) {
                detectTransformGestures(panZoomLock = false) { centroid, panDelta, zoomDelta, _ ->
                    val oldZoom = zoom
                    val newZoom = (zoom * zoomDelta).coerceIn(0.5f, 25.0f)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    pan = (pan + panDelta) + (centroid - (center + pan)) * (1f - newZoom / oldZoom)
                    zoom = newZoom
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Draw Land Masses
            drawLandMasses(
                type = config.mapProjection,
                homeLat = config.homeLat,
                homeLon = config.homeLon,
                w = w,
                h = h,
                zoom = zoom,
                pan = pan,
                fill = landFillColor,
                stroke = landStrokeColor
            )

            // 2. Draw Solar Terminator (Grayline) if enabled
            if (config.showGrayline) {
                drawSolarTerminator(
                    type = config.mapProjection,
                    homeLat = config.homeLat,
                    homeLon = config.homeLon,
                    w = w,
                    h = h,
                    zoom = zoom,
                    pan = pan,
                    nightColor = graylineColor,
                    terminatorColor = terminatorLineColor
                )
            }

            // 3. Draw Projection Overlays (AEQD concentric rings and azimuth rays)
            if (config.showRings && config.mapProjection == MapProjectionType.AEQD) {
                drawAeqdOverlays(
                    homeLat = config.homeLat,
                    homeLon = config.homeLon,
                    radiusKm = config.radiusKm,
                    w = w,
                    h = h,
                    zoom = zoom,
                    pan = pan,
                    ringColor = ringColor,
                    textMeasurer = textMeasurer
                )
            }

            // 4. Draw Home Marker
            val homeScreenPos = MapProjectionEngine.project(
                lat = config.homeLat,
                lon = config.homeLon,
                type = config.mapProjection,
                homeLat = config.homeLat,
                homeLon = config.homeLon,
                width = w,
                height = h,
                zoom = zoom,
                pan = pan
            )

            drawCircle(
                color = Color(0xFF10B981),
                radius = 8f,
                center = homeScreenPos
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = homeScreenPos
            )

            // 5. Draw Great Circle Path for selected spot if available
            if (selectedSpot != null) {
                drawGreatCirclePath(
                    lat1 = config.homeLat,
                    lon1 = config.homeLon,
                    lat2 = selectedSpot.lat,
                    lon2 = selectedSpot.lon,
                    type = config.mapProjection,
                    homeLat = config.homeLat,
                    homeLon = config.homeLon,
                    w = w,
                    h = h,
                    zoom = zoom,
                    pan = pan,
                    color = Color(0xFFF59E0B)
                )
            }

            // 6. Project and Draw Spot Dots
            val liveProjected = mutableListOf<Pair<Spot, Offset>>()
            for (spot in spots) {
                // Exclude any spots that do not belong to currently selected bands
                if (config.selectedBands.isNotEmpty() && !config.selectedBands.contains(spot.band)) {
                    continue
                }

                val screenPos = MapProjectionEngine.project(
                    lat = spot.lat,
                    lon = spot.lon,
                    type = config.mapProjection,
                    homeLat = config.homeLat,
                    homeLon = config.homeLon,
                    width = w,
                    height = h,
                    zoom = zoom,
                    pan = pan
                )

                // Skip if offscreen
                if (screenPos.x in -50f..(w + 50f) && screenPos.y in -50f..(h + 50f)) {
                    liveProjected.add(spot to screenPos)

                    val bandObj = HamBand.fromString(spot.band)
                    val dotColor = bandObj?.color ?: Color(0xFF38BDF8)
                    // Scaled dot radius
                    val baseRadius = spot.getDotRadiusDp(config.dotScale) * density

                    val isSelected = selectedSpot?.id == spot.id
                    if (isSelected) {
                        drawCircle(
                            color = Color.White,
                            radius = baseRadius + 4f,
                            center = screenPos
                        )
                        drawCircle(
                            color = Color(0xFFF59E0B),
                            radius = baseRadius + 2.5f,
                            center = screenPos
                        )
                    }

                    drawCircle(
                        color = Color(0xCC000000),
                        radius = baseRadius + 0.8f,
                        center = screenPos
                    )
                    drawCircle(
                        color = dotColor,
                        radius = baseRadius,
                        center = screenPos
                    )
                }
            }
            projectedSpots = liveProjected

            // 7. Watermark
            drawWatermark(w, h, textMeasurer)
        }

        // 8. Floating Map Zoom and Navigation Controls (Center Right)
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).copy(alpha = 0.92f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Zoom in (+)
                IconButton(
                    onClick = { zoom = (zoom * 1.4f).coerceAtMost(25.0f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                }

                // Current Zoom factor
                Text(
                    text = String.format("%.1f×", zoom),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                // Zoom out (-)
                IconButton(
                    onClick = { zoom = (zoom / 1.4f).coerceAtLeast(0.5f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                }

                HorizontalDivider(modifier = Modifier.width(24.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Recenter on Home
                IconButton(
                    onClick = { pan = Offset.Zero },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center Home", modifier = Modifier.size(18.dp))
                }

                // Reset View
                IconButton(
                    onClick = {
                        zoom = 1f
                        pan = Offset.Zero
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset View", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun DrawScope.drawLandMasses(
    type: MapProjectionType,
    homeLat: Double,
    homeLon: Double,
    w: Float,
    h: Float,
    zoom: Float,
    pan: Offset,
    fill: Color,
    stroke: Color
) {
    for (poly in WorldMapData.LAND_POLYGONS) {
        if (poly.isEmpty()) continue
        val path = Path()
        var first = true

        for (pt in poly) {
            val offset = MapProjectionEngine.project(
                lat = pt.first,
                lon = pt.second,
                type = type,
                homeLat = homeLat,
                homeLon = homeLon,
                width = w,
                height = h,
                zoom = zoom,
                pan = pan
            )

            if (first) {
                path.moveTo(offset.x, offset.y)
                first = false
            } else {
                path.lineTo(offset.x, offset.y)
            }
        }
        path.close()

        drawPath(path = path, color = fill)
        drawPath(path = path, color = stroke, style = Stroke(width = 1.2f))
    }
}

private fun DrawScope.drawAeqdOverlays(
    homeLat: Double,
    homeLon: Double,
    radiusKm: Double,
    w: Float,
    h: Float,
    zoom: Float,
    pan: Offset,
    ringColor: Color,
    textMeasurer: TextMeasurer
) {
    val cx = w / 2f + pan.x
    val cy = h / 2f + pan.y
    val maxDistKm = Math.PI * GeoMath.EARTH_RADIUS_KM
    val mapRadius = (min(w, h) / 2f - 24f) * zoom

    // Draw cardinal direction radial lines (N, E, S, W, and 45° bearings)
    for (bearing in 0 until 360 step 45) {
        val rad = Math.toRadians(bearing.toDouble())
        val ex = cx + mapRadius * sin(rad).toFloat()
        val ey = cy - mapRadius * cos(rad).toFloat()

        drawLine(
            color = Color(0x3364748B),
            start = Offset(cx, cy),
            end = Offset(ex, ey),
            strokeWidth = 1f
        )

        // Cardinal labels
        val label = when (bearing) {
            0 -> "000° N"
            45 -> "045° NE"
            90 -> "090° E"
            135 -> "135° SE"
            180 -> "180° S"
            225 -> "225° SW"
            270 -> "270° W"
            315 -> "315° NW"
            else -> "$bearing°"
        }

        val lx = cx + (mapRadius + 18f) * sin(rad).toFloat() - 20f
        val ly = cy - (mapRadius + 18f) * cos(rad).toFloat() - 10f

        drawSafeText(
            textMeasurer = textMeasurer,
            text = label,
            x = lx,
            y = ly,
            style = TextStyle(
                color = Color(0x8894A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }

    // Concentric range rings: 2500km, 5000km, 10000km, 15000km, 20000km
    val ringDistances = listOf(1000.0, 2500.0, 5000.0, 10000.0, 15000.0, 20000.0)
    for (dKm in ringDistances) {
        val r = (dKm / maxDistKm).toFloat() * mapRadius
        if (r > 0f) {
            drawCircle(
                color = ringColor,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1f)
            )

            // Km label on North axis
            val label = "${(dKm / 1000).toInt()}k km"
            drawSafeText(
                textMeasurer = textMeasurer,
                text = label,
                x = cx + 4f,
                y = cy - r - 6f,
                style = TextStyle(
                    color = Color(0x9938BDF8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }

    // Highlight user's configured filter radius circle
    if (radiusKm in 50.0..19000.0) {
        val rRadius = (radiusKm / maxDistKm).toFloat() * mapRadius
        drawCircle(
            color = Color(0xFFEF4444),
            radius = rRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
        drawSafeText(
            textMeasurer = textMeasurer,
            text = "Filter: ${radiusKm.toInt()} km",
            x = cx + 8f,
            y = cy - rRadius + 2f,
            style = TextStyle(
                color = Color(0xFFEF4444),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun DrawScope.drawSolarTerminator(
    type: MapProjectionType,
    homeLat: Double,
    homeLon: Double,
    w: Float,
    h: Float,
    zoom: Float,
    pan: Offset,
    nightColor: Color,
    terminatorColor: Color
) {
    val solarCoords = SolarTerminator.computeSolarCoordinates()

    // Sample night points and terminator boundary
    val terminatorPoints = mutableListOf<Offset>()
    for (lonDeg in -180..180 step 6) {
        var bestLat = 0.0
        var minDiff = 999.0
        for (latDeg in -85..85 step 3) {
            val alt = SolarTerminator.getSolarAltitude(latDeg.toDouble(), lonDeg.toDouble(), solarCoords)
            if (abs(alt) < minDiff) {
                minDiff = abs(alt)
                bestLat = latDeg.toDouble()
            }
        }
        val pt = MapProjectionEngine.project(
            lat = bestLat,
            lon = lonDeg.toDouble(),
            type = type,
            homeLat = homeLat,
            homeLon = homeLon,
            width = w,
            height = h,
            zoom = zoom,
            pan = pan
        )
        terminatorPoints.add(pt)
    }

    // Draw twilight terminator line
    for (i in 0 until terminatorPoints.size - 1) {
        val p1 = terminatorPoints[i]
        val p2 = terminatorPoints[i + 1]
        if (hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble()) < w * 0.4) {
            drawLine(
                color = terminatorColor,
                start = p1,
                end = p2,
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
    }

    // Mark Subsolar (Sun) and Antipodal (Night Center) points
    val sunScreen = MapProjectionEngine.project(
        lat = solarCoords.subsolarLat,
        lon = solarCoords.subsolarLon,
        type = type,
        homeLat = homeLat,
        homeLon = homeLon,
        width = w,
        height = h,
        zoom = zoom,
        pan = pan
    )
    drawCircle(color = Color(0xFFFBBF24), radius = 6f, center = sunScreen)
    drawCircle(color = Color(0xFFFEF08A), radius = 3f, center = sunScreen)
}

private fun DrawScope.drawGreatCirclePath(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
    type: MapProjectionType,
    homeLat: Double,
    homeLon: Double,
    w: Float,
    h: Float,
    zoom: Float,
    pan: Offset,
    color: Color
) {
    val arc = GeoMath.greatCircleArc(lat1, lon1, lat2, lon2, numSegments = 32)
    for (i in 0 until arc.size - 1) {
        val pt1 = MapProjectionEngine.project(
            lat = arc[i].first,
            lon = arc[i].second,
            type = type,
            homeLat = homeLat,
            homeLon = homeLon,
            width = w,
            height = h,
            zoom = zoom,
            pan = pan
        )
        val pt2 = MapProjectionEngine.project(
            lat = arc[i + 1].first,
            lon = arc[i + 1].second,
            type = type,
            homeLat = homeLat,
            homeLon = homeLon,
            width = w,
            height = h,
            zoom = zoom,
            pan = pan
        )

        if (hypot((pt2.x - pt1.x).toDouble(), (pt2.y - pt1.y).toDouble()) < w * 0.5) {
            drawLine(
                color = color,
                start = pt1,
                end = pt2,
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawWatermark(w: Float, h: Float, textMeasurer: TextMeasurer) {
    val watermarkText = "PSKProp • by OH2XX"
    drawSafeText(
        textMeasurer = textMeasurer,
        text = watermarkText,
        x = w - 140f,
        y = h - 28f,
        style = TextStyle(
            color = Color(0x66FFFFFF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    )
}

private fun DrawScope.drawSafeText(
    textMeasurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    style: TextStyle
) {
    try {
        val layoutResult = textMeasurer.measure(text, style)
        val textW = layoutResult.size.width.toFloat()
        val textH = layoutResult.size.height.toFloat()

        if (x + textW > 0f && x < size.width && y + textH > 0f && y < size.height) {
            val clampedX = x.coerceIn(4f, (size.width - textW - 4f).coerceAtLeast(4f))
            val clampedY = y.coerceIn(4f, (size.height - textH - 4f).coerceAtLeast(4f))
            drawText(
                textLayoutResult = layoutResult,
                topLeft = Offset(clampedX, clampedY)
            )
        }
    } catch (_: Exception) {
        // Safe fallback
    }
}
