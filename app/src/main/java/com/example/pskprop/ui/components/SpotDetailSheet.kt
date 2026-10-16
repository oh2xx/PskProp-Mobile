package com.example.pskprop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pskprop.model.HamBand
import com.example.pskprop.model.Spot
import com.example.pskprop.model.SpotRole
import com.example.pskprop.util.GeoMath
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SpotDetailSheet(
    spot: Spot,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bandObj = HamBand.fromString(spot.band)
    val bandColor = bandObj?.color ?: MaterialTheme.colorScheme.primary
    val distMiles = spot.distanceKm * GeoMath.KM_TO_MILES
    val compassHeading = GeoMath.compassHeading(spot.bearingDeg)

    val timeFormatted = rememberTimeFormatted(spot.timestampSeconds)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Callsign, Role Badge, and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(bandColor)
                    )
                    Text(
                        text = spot.callsign,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Role Pill
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = if (spot.kind == SpotRole.SENDER) "TX Heard" else "RX Spotted",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (spot.kind == SpotRole.SENDER) Icons.Default.Podcasts else Icons.Default.Hearing,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (spot.kind == SpotRole.SENDER) Color(0x3310B981) else Color(0x3338BDF8),
                            labelColor = if (spot.kind == SpotRole.SENDER) Color(0xFF10B981) else Color(0xFF38BDF8),
                            leadingIconContentColor = if (spot.kind == SpotRole.SENDER) Color(0xFF10B981) else Color(0xFF38BDF8)
                        )
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Grid square and DXCC Country
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = spot.locator,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                if (!spot.dxcc.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = spot.dxcc,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Propagation Stats Grid: Distance, Bearing, SNR, Band/Freq
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Distance
                PropStatItem(
                    label = "Great Circle",
                    value = "${spot.distanceKm.toInt()} km",
                    subValue = "${distMiles.toInt()} mi",
                    icon = Icons.Default.Explore
                )

                // Azimuth Bearing
                PropStatItem(
                    label = "Bearing",
                    value = "${spot.bearingDeg.toInt()}°",
                    subValue = compassHeading,
                    icon = Icons.Default.Navigation,
                    iconRotation = spot.bearingDeg.toFloat()
                )

                // SNR
                PropStatItem(
                    label = "Report (SNR)",
                    value = spot.snrDisplay,
                    subValue = if ((spot.snr ?: -99) >= 0) "Strong" else if ((spot.snr ?: -99) >= -15) "Good" else "Weak",
                    icon = Icons.Default.SignalCellularAlt,
                    accentColor = when {
                        spot.snr == null -> MaterialTheme.colorScheme.outline
                        spot.snr >= 0 -> Color(0xFF10B981)
                        spot.snr >= -15 -> Color(0xFFFBBF24)
                        else -> Color(0xFFEF4444)
                    }
                )

                // Band & Mode
                PropStatItem(
                    label = "Band / Mode",
                    value = spot.band,
                    subValue = spot.mode,
                    icon = Icons.Default.Radio,
                    accentColor = bandColor
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Partner Station QSO info & timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Partner: ${spot.partnerCallsign} (${spot.partnerLocator})",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
}

@Composable
private fun PropStatItem(
    label: String,
    value: String,
    subValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconRotation: Float = 0f,
    accentColor: Color? = null
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(13.dp)
                    .rotate(iconRotation),
                tint = accentColor ?: MaterialTheme.colorScheme.outline
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp
                )
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = accentColor ?: MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text = subValue,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun rememberTimeFormatted(epochSeconds: Long): String {
    val now = System.currentTimeMillis() / 1000L
    val diffSec = now - epochSeconds
    return when {
        diffSec < 60 -> "Just now"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        diffSec < 86400 -> "${diffSec / 3600}h ago"
        else -> {
            val sdf = SimpleDateFormat("HH:mm UTC", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(Date(epochSeconds * 1000L))
        }
    }
}
