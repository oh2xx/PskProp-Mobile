package com.example.pskprop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pskprop.model.DotSizeOption
import com.example.pskprop.model.MapProjectionType
import com.example.pskprop.util.MaidenheadUtil
import com.example.pskprop.viewmodel.PskPropViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PskPropViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()

    var homeGridInput by remember(config.homeLocator) { mutableStateOf(config.homeLocator) }

    val autoRefreshOptions = listOf(0 to "Off (Manual)", 30 to "Every 30s", 60 to "Every 1 min", 120 to "Every 2 mins", 300 to "Every 5 mins")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Configuration",
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
            // Station Settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Home Station Locator", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    OutlinedTextField(
                        value = homeGridInput,
                        onValueChange = { input ->
                            homeGridInput = input.uppercase()
                            if (MaidenheadUtil.isValidLocator(input)) {
                                viewModel.updateHomeLocator(input)
                            }
                        },
                        label = { Text("Maidenhead Locator (Grid Square)") },
                        placeholder = { Text("e.g. KP20") },
                        singleLine = true,
                        supportingText = {
                            Text(
                                "Coordinates: ${String.format("%.4f° N, %.4f° E", config.homeLat, config.homeLon)}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Map and Projection Settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Map & Visuals", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    // Projection selection
                    Text("Map Projection", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    MapProjectionType.entries.forEach { proj ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = config.mapProjection == proj,
                                onClick = { viewModel.updateMapProjection(proj) }
                            )
                            Column {
                                Text(proj.label, style = MaterialTheme.typography.bodyMedium)
                                Text(proj.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    HorizontalDivider()

                    // Grayline switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Solar Grayline Terminator", style = MaterialTheme.typography.bodyMedium)
                            Text("Show real-time day/night twilight boundary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = config.showGrayline,
                            onCheckedChange = { viewModel.toggleGrayline() }
                        )
                    }

                    // Distance rings switch
                    if (config.mapProjection == MapProjectionType.AEQD) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Azimuthal Distance Rings", style = MaterialTheme.typography.bodyMedium)
                                Text("Concentric range circles and azimuth radials", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Switch(
                                checked = config.showRings,
                                onCheckedChange = { viewModel.toggleRings() }
                            )
                        }
                    }

                    // Map Theme switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Map Theme", style = MaterialTheme.typography.bodyMedium)
                            Text("Deep slate ocean with illuminated dots", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = config.darkMapTheme,
                            onCheckedChange = { viewModel.toggleMapTheme() }
                        )
                    }

                    HorizontalDivider()

                    // Dot Size configuration
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Spot Dot Diameter", style = MaterialTheme.typography.bodyMedium)
                                Text("Scale size of spotted stations on the map", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Text(
                                String.format("%.2f×", config.dotScale),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DotSizeOption.entries.forEach { opt ->
                                val isSel = kotlin.math.abs(config.dotScale - opt.scale) < 0.08f
                                FilterChip(
                                    selected = isSel,
                                    onClick = { viewModel.updateDotScale(opt.scale) },
                                    label = { Text(opt.label, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Slider(
                            value = config.dotScale,
                            onValueChange = { viewModel.updateDotScale(it) },
                            valueRange = 0.25f..1.50f,
                            steps = 12
                        )
                    }
                }
            }

            // Auto Refresh Settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Live Polling / Auto-Refresh", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    autoRefreshOptions.forEach { (sec, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = config.autoRefreshSeconds == sec,
                                onClick = { viewModel.updateAutoRefresh(sec) }
                            )
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // About & Credits Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("About PSKProp", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Text(
                        "PSKProp is a standalone native Android HF propagation monitor that queries pskreporter.info reception reports and calculates amateur radio propagation conditions, Maidenhead locators, and great circle trajectories in real time.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "Based on the pskprop system by Kari Lehtimäki (OH2XX).",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        "Data source: PSKReporter.info (Philip Gladstone).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
