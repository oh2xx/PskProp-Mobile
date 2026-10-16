package com.example.pskprop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pskprop.model.DotSizeOption
import com.example.pskprop.model.MapProjectionType
import com.example.pskprop.model.PropConfig
import com.example.pskprop.model.QueryDirection
import com.example.pskprop.model.Spot
import com.example.pskprop.ui.components.BandFilterBar
import com.example.pskprop.ui.components.SpotDetailSheet
import com.example.pskprop.ui.map.PropagationMap
import com.example.pskprop.util.MaidenheadUtil
import com.example.pskprop.viewmodel.PskPropViewModel

@Composable
fun MapScreen(
    viewModel: PskPropViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val spots by viewModel.spots.collectAsState()
    val selectedSpot by viewModel.selectedSpot.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()

    val focusManager = LocalFocusManager.current

    var homeGridInput by remember(config.homeLocator) { mutableStateOf(config.homeLocator) }
    var callsignInput by remember(config.callsignFilter) { mutableStateOf(config.callsignFilter) }

    var showRadiusDialog by remember { mutableStateOf(false) }
    var showAgeDialog by remember { mutableStateOf(false) }
    var showProjectionMenu by remember { mutableStateOf(false) }
    var showDirectionMenu by remember { mutableStateOf(false) }
    var showDotSizeDialog by remember { mutableStateOf(false) }

    // Count spots per band
    val spotCounts = remember(spots) {
        spots.groupingBy { it.band }.eachCount()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. The Interactive Propagation Canvas Map
        PropagationMap(
            config = config,
            spots = spots,
            selectedSpot = selectedSpot,
            onSpotSelected = { viewModel.selectSpot(it) },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Controls Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Parameter Input Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = 0.94f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Row 1: Home Grid & Callsign & Query Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Home Grid input
                        OutlinedTextField(
                            value = homeGridInput,
                            onValueChange = { input ->
                                homeGridInput = input.uppercase()
                                if (MaidenheadUtil.isValidLocator(input)) {
                                    viewModel.updateHomeLocator(input)
                                }
                            },
                            label = { Text("Home Grid", fontSize = 11.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.updateHomeLocator(homeGridInput)
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        )

                        // Callsign Filter input
                        OutlinedTextField(
                            value = callsignInput,
                            onValueChange = { input ->
                                callsignInput = input.uppercase()
                                viewModel.updateCallsignFilter(input)
                            },
                            label = { Text("Callsign Filter", fontSize = 11.sp) },
                            placeholder = { Text("e.g. OH2XX", fontSize = 11.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Podcasts,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    viewModel.fetchSpots()
                                }
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(56.dp)
                        )

                        // Query / Refresh Button
                        FilledTonalButton(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.fetchSpots()
                            },
                            enabled = !isLoading,
                            modifier = Modifier.height(56.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Query PSK Reporter"
                                )
                            }
                        }
                    }

                    // Row 2: Secondary Filters (Projection, Radius, Age, Grayline)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Projection chip
                        FilterChip(
                            selected = true,
                            onClick = { showProjectionMenu = true },
                            label = { Text(config.mapProjection.label, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                        DropdownMenu(
                            expanded = showProjectionMenu,
                            onDismissRequest = { showProjectionMenu = false }
                        ) {
                            MapProjectionType.entries.forEach { proj ->
                                DropdownMenuItem(
                                    text = { Text(proj.label) },
                                    onClick = {
                                        viewModel.updateMapProjection(proj)
                                        showProjectionMenu = false
                                    }
                                )
                            }
                        }

                        // Radius Chip
                        FilterChip(
                            selected = config.radiusKm < 19000.0,
                            onClick = { showRadiusDialog = true },
                            label = {
                                val text = if (config.radiusKm >= 19000.0) "Radius: Global" else "Radius: ${config.radiusKm.toInt()} km"
                                Text(text, fontSize = 11.sp)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )

                        // Direction Chip (Both / TX / RX)
                        FilterChip(
                            selected = config.queryDirection != QueryDirection.BOTH,
                            onClick = { showDirectionMenu = true },
                            label = { Text(config.queryDirection.label, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                        DropdownMenu(
                            expanded = showDirectionMenu,
                            onDismissRequest = { showDirectionMenu = false }
                        ) {
                            QueryDirection.entries.forEach { dir ->
                                DropdownMenuItem(
                                    text = { Text(dir.label) },
                                    onClick = {
                                        viewModel.updateQueryDirection(dir)
                                        showDirectionMenu = false
                                    }
                                )
                            }
                        }

                        // Age Chip
                        FilterChip(
                            selected = true,
                            onClick = { showAgeDialog = true },
                            label = { Text("Age: ${config.ageMinutes}m", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )

                        // Grayline Toggle Chip
                        FilterChip(
                            selected = config.showGrayline,
                            onClick = { viewModel.toggleGrayline() },
                            label = { Text("Grayline", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (config.showGrayline) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (config.showGrayline) Color(0xFFFBBF24) else MaterialTheme.colorScheme.outline
                                )
                            }
                        )

                        // Range Rings Toggle
                        if (config.mapProjection == MapProjectionType.AEQD) {
                            FilterChip(
                                selected = config.showRings,
                                onClick = { viewModel.toggleRings() },
                                label = { Text("Rings", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // Dot Size Chip
                        FilterChip(
                            selected = true,
                            onClick = { showDotSizeDialog = true },
                            label = {
                                val sizeLabel = when {
                                    config.dotScale <= 0.40f -> "Dots: Tiny"
                                    config.dotScale <= 0.60f -> "Dots: Small"
                                    config.dotScale <= 0.85f -> "Dots: Med"
                                    else -> "Dots: Large"
                                }
                                Text(sizeLabel, fontSize = 11.sp)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Adjust, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )

                        // Map Theme Toggle
                        IconButton(
                            onClick = { viewModel.toggleMapTheme() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (config.darkMapTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Toggle theme",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Band Selector Bar
            BandFilterBar(
                selectedBands = config.selectedBands,
                spotCountsByBand = spotCounts,
                onToggleBand = { viewModel.toggleBand(it) },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.90f))
            )
        }

        // 3. Status Bar & Error Notification
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = if (selectedSpot != null) 230.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Error banner
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer)
                        )
                        TextButton(
                            onClick = { viewModel.loadSampleDataset() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Sample Data", fontSize = 11.sp)
                        }
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Spots Count & Status Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (spots.isNotEmpty()) Color(0xFF10B981) else Color(0xFFF59E0B))
                    )
                    Text(
                        text = "${spots.size} spots",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                    Text(
                        text = "Home: ${config.homeLocator}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }
        }

        // 4. Selected Spot Detail Card at bottom
        AnimatedVisibility(
            visible = selectedSpot != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedSpot?.let { spot ->
                SpotDetailSheet(
                    spot = spot,
                    onDismiss = { viewModel.selectSpot(null) }
                )
            }
        }
    }

    // Radius Picker Dialog
    if (showRadiusDialog) {
        val radiusOptions = listOf(100.0, 200.0, 400.0, 800.0, 1500.0, 3000.0, 6000.0, 20000.0)
        AlertDialog(
            onDismissRequest = { showRadiusDialog = false },
            title = { Text("Filter Radius from Home (${config.homeLocator})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Shows transmitter/receiver traffic originating or terminating within this radius.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    radiusOptions.forEach { r ->
                        val label = if (r >= 19000.0) "Global (No radius limit)" else "${r.toInt()} km"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = config.radiusKm == r,
                                onClick = {
                                    viewModel.updateRadiusKm(r)
                                    showRadiusDialog = false
                                }
                            )
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRadiusDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Age Picker Dialog
    if (showAgeDialog) {
        val ageOptions = listOf(5, 10, 15, 30, 60, 120, 240)
        AlertDialog(
            onDismissRequest = { showAgeDialog = false },
            title = { Text("Maximum Spot Age") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Retrieve reports reported within the last N minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ageOptions.forEach { ageMin ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = config.ageMinutes == ageMin,
                                onClick = {
                                    viewModel.updateAgeMinutes(ageMin)
                                    showAgeDialog = false
                                }
                            )
                            Text("$ageMin minutes", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAgeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dot Size Picker Dialog
    if (showDotSizeDialog) {
        AlertDialog(
            onDismissRequest = { showDotSizeDialog = false },
            title = { Text("Spot Dot Diameter / Size") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Scale the diameter of reception spot dots on the map to balance visual density and map readability.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Quick presets
                    Text(
                        "Preset Sizes:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DotSizeOption.entries.forEach { opt ->
                            val isSel = kotlin.math.abs(config.dotScale - opt.scale) < 0.08f
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.updateDotScale(opt.scale) },
                                label = { Text(opt.label, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Fine tuning Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Fine Size Adjustment:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            String.format("%.2f×", config.dotScale),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Slider(
                        value = config.dotScale,
                        onValueChange = { viewModel.updateDotScale(it) },
                        valueRange = 0.25f..1.50f,
                        steps = 12
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDotSizeDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
