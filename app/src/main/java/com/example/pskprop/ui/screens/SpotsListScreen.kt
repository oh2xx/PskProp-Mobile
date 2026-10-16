package com.example.pskprop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pskprop.model.HamBand
import com.example.pskprop.model.Spot
import com.example.pskprop.model.SpotRole
import com.example.pskprop.ui.components.SpotDetailSheet
import com.example.pskprop.util.GeoMath
import com.example.pskprop.viewmodel.PskPropViewModel

enum class SpotSortOrder(val label: String) {
    TIME_DESC("Newest First"),
    DIST_DESC("Furthest (Max DX)"),
    DIST_ASC("Closest"),
    SNR_DESC("Strongest Signal"),
    CALLSIGN_ASC("Callsign (A-Z)")
}

@Composable
fun SpotsListScreen(
    viewModel: PskPropViewModel,
    onNavigateToMap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val spots by viewModel.spots.collectAsState()
    val selectedSpot by viewModel.selectedSpot.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SpotSortOrder.TIME_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredSpots = remember(spots, searchQuery, sortOrder, config.selectedBands) {
        val q = searchQuery.trim().uppercase()
        val bandFiltered = spots.filter { spot ->
            config.selectedBands.isEmpty() || config.selectedBands.contains(spot.band)
        }
        val list = if (q.isEmpty()) {
            bandFiltered
        } else {
            bandFiltered.filter {
                it.callsign.contains(q, ignoreCase = true) ||
                        it.locator.contains(q, ignoreCase = true) ||
                        it.band.contains(q, ignoreCase = true) ||
                        (it.dxcc?.contains(q, ignoreCase = true) == true)
            }
        }

        when (sortOrder) {
            SpotSortOrder.TIME_DESC -> list.sortedByDescending { it.timestampSeconds }
            SpotSortOrder.DIST_DESC -> list.sortedByDescending { it.distanceKm }
            SpotSortOrder.DIST_ASC -> list.sortedBy { it.distanceKm }
            SpotSortOrder.SNR_DESC -> list.sortedByDescending { it.snr ?: -99 }
            SpotSortOrder.CALLSIGN_ASC -> list.sortedBy { it.callsign }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Reception Spots",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${filteredSpots.size} of ${spots.size} spots",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { viewModel.fetchSpots() }, enabled = !isLoading) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }

                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SpotSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label) },
                                        leadingIcon = {
                                            if (sortOrder == order) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        },
                                        onClick = {
                                            sortOrder = order
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search callsign, grid, country...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (filteredSpots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Podcasts,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No spots match your filter",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Try adjusting your search query, increasing radius, or enabling more bands.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Button(onClick = { viewModel.loadSampleDataset() }) {
                            Text("Load Sample Spots")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSpots, key = { it.id }) { spot ->
                        SpotItemCard(
                            spot = spot,
                            isSelected = selectedSpot?.id == spot.id,
                            onClick = { viewModel.selectSpot(spot) }
                        )
                    }
                }
            }

            // Bottom detail sheet if one is tapped
            selectedSpot?.let { spot ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    SpotDetailSheet(
                        spot = spot,
                        onDismiss = { viewModel.selectSpot(null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotItemCard(
    spot: Spot,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bandObj = HamBand.fromString(spot.band)
    val bandColor = bandObj?.color ?: MaterialTheme.colorScheme.primary
    val compass = GeoMath.compassHeading(spot.bearingDeg)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Band Indicator Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(bandColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = spot.band,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = bandColor
                        )
                    )
                }
                Text(
                    text = spot.mode,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                )
            }

            // Station and location info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = spot.callsign,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = "[${spot.locator}]",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                if (!spot.dxcc.isNullOrBlank()) {
                    Text(
                        text = spot.dxcc,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (spot.kind == SpotRole.SENDER) "TX" else "RX",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (spot.kind == SpotRole.SENDER) Color(0xFF10B981) else Color(0xFF38BDF8)
                        )
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                    Text(
                        text = "${spot.distanceKm.toInt()} km",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier
                                .size(11.dp)
                                .rotate(spot.bearingDeg.toFloat()),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${spot.bearingDeg.toInt()}° $compass",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                    }
                }
            }

            // SNR Badge on right
            val snrVal = spot.snr ?: -99
            val snrColor = when {
                snrVal >= 0 -> Color(0xFF10B981)
                snrVal >= -15 -> Color(0xFFFBBF24)
                else -> Color(0xFFEF4444)
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = snrColor.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, snrColor.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = spot.snrDisplay,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = snrColor
                        )
                    )
                    Text(
                        text = "SNR",
                        style = TextStyle(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal,
                            color = snrColor
                        )
                    )
                }
            }
        }
    }
}
