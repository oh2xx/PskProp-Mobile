package com.example.pskprop.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pskprop.util.GeoMath
import com.example.pskprop.util.MaidenheadUtil
import com.example.pskprop.viewmodel.PskPropViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzimuthCalcScreen(
    viewModel: PskPropViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val focusManager = LocalFocusManager.current

    var fromGrid by remember(config.homeLocator) { mutableStateOf(config.homeLocator) }
    var toGrid by remember { mutableStateOf("PM95sq") }

    val fromCoords = remember(fromGrid) { MaidenheadUtil.maidenheadToLatLon(fromGrid) }
    val toCoords = remember(toGrid) { MaidenheadUtil.maidenheadToLatLon(toGrid) }

    val calcResult = remember(fromCoords, toCoords) {
        if (fromCoords != null && toCoords != null) {
            val distKm = GeoMath.haversineKm(fromCoords.first, fromCoords.second, toCoords.first, toCoords.second)
            val distMiles = distKm * GeoMath.KM_TO_MILES
            val bearing = GeoMath.initialBearing(fromCoords.first, fromCoords.second, toCoords.first, toCoords.second)
            val backBearing = GeoMath.backBearing(fromCoords.first, fromCoords.second, toCoords.first, toCoords.second)
            val compass = GeoMath.compassHeading(bearing)
            val backCompass = GeoMath.compassHeading(backBearing)
            AzimuthResult(distKm, distMiles, bearing, backBearing, compass, backCompass)
        } else null
    }

    val quickPresets = listOf(
        "Tokyo (PM95)" to "PM95",
        "New York (FN31)" to "FN31",
        "London (IO91)" to "IO91",
        "Sydney (QF56)" to "QF56",
        "Los Angeles (DM04)" to "DM04",
        "Rio (GG87)" to "GG87"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Azimuth & Distance",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Inputs Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Great Circle Path Calculator",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = fromGrid,
                            onValueChange = { fromGrid = it.uppercase() },
                            label = { Text("From (Origin)") },
                            placeholder = { Text("e.g. KP20") },
                            singleLine = true,
                            isError = fromCoords == null && fromGrid.isNotBlank(),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = toGrid,
                            onValueChange = { toGrid = it.uppercase() },
                            label = { Text("To (Destination)") },
                            placeholder = { Text("e.g. FN31") },
                            singleLine = true,
                            isError = toCoords == null && toGrid.isNotBlank(),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Presets
                    Text(
                        text = "Quick Destination Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickPresets.take(3).forEach { (label, grid) ->
                            AssistChip(
                                onClick = { toGrid = grid },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickPresets.drop(3).forEach { (label, grid) ->
                            AssistChip(
                                onClick = { toGrid = grid },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }

            // Results View
            if (calcResult != null && fromCoords != null && toCoords != null) {
                // Beam Compass Dial
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Beam Heading / Antenna Azimuth",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        // Compass Dial Canvas
                        Box(
                            modifier = Modifier.size(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CompassDial(
                                bearingDeg = calcResult.bearingDeg.toFloat(),
                                modifier = Modifier.fillMaxSize()
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${calcResult.bearingDeg.toInt()}°",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = calcResult.compass,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // Distance and Heading Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${calcResult.distKm.toInt()} km",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${calcResult.distMiles.toInt()} miles",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${calcResult.backBearingDeg.toInt()}°",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Back Bearing (${calcResult.backCompass})",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                                )
                            }
                        }
                    }
                }

                // Coordinates Detail
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Geographic Coordinates",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Origin ($fromGrid):", style = MaterialTheme.typography.bodySmall)
                            Text(
                                String.format("%.4f°, %.4f°", fromCoords.first, fromCoords.second),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Target ($toGrid):", style = MaterialTheme.typography.bodySmall)
                            Text(
                                String.format("%.4f°, %.4f°", toCoords.first, toCoords.second),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = "Enter valid 2, 4, 6, or 8-character Maidenhead locators (e.g. KP20, FN31pr).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompassDial(
    bearingDeg: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(cx, cy) - 12f

        // Outer ring
        drawCircle(
            color = Color(0x3364748B),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )

        // Tick marks every 30 degrees
        for (deg in 0 until 360 step 30) {
            val rad = Math.toRadians(deg.toDouble())
            val innerR = if (deg % 90 == 0) r - 12f else r - 6f
            val sx = cx + innerR * sin(rad).toFloat()
            val sy = cy - innerR * cos(rad).toFloat()
            val ex = cx + r * sin(rad).toFloat()
            val ey = cy - r * cos(rad).toFloat()

            drawLine(
                color = if (deg % 90 == 0) Color(0xFF38BDF8) else Color(0x6694A3B8),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = if (deg % 90 == 0) 2.5f else 1.5f,
                cap = StrokeCap.Round
            )
        }

        // Antenna Beam Arrow Pointer
        val arrowRad = Math.toRadians(bearingDeg.toDouble())
        val tipX = cx + (r - 18f) * sin(arrowRad).toFloat()
        val tipY = cy - (r - 18f) * cos(arrowRad).toFloat()

        val baseLeftRad = Math.toRadians((bearingDeg - 140).toDouble())
        val baseRightRad = Math.toRadians((bearingDeg + 140).toDouble())
        val baseR = 24f

        val p1 = Offset(tipX, tipY)
        val p2 = Offset(cx + baseLeftRad.let { sin(it).toFloat() * baseR }, cy - baseLeftRad.let { cos(it).toFloat() * baseR })
        val p3 = Offset(cx + baseRightRad.let { sin(it).toFloat() * baseR }, cy - baseRightRad.let { cos(it).toFloat() * baseR })

        val path = Path().apply {
            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(cx, cy)
            lineTo(p3.x, p3.y)
            close()
        }

        drawPath(path = path, color = Color(0xFFEF4444))
        drawCircle(color = Color.White, radius = 5f, center = Offset(cx, cy))
    }
}

private data class AzimuthResult(
    val distKm: Double,
    val distMiles: Double,
    val bearingDeg: Double,
    val backBearingDeg: Double,
    val compass: String,
    val backCompass: String
)
