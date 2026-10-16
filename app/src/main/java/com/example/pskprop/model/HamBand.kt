package com.example.pskprop.model

import androidx.compose.ui.graphics.Color

enum class HamBand(
    val label: String,
    val minFreqHz: Long,
    val maxFreqHz: Long,
    val color: Color
) {
    BAND_160M("160m", 1_800_000L, 2_000_000L, Color(0xFF8B4513)),
    BAND_80M("80m", 3_500_000L, 4_000_000L, Color(0xFF4B0082)),
    BAND_60M("60m", 5_000_000L, 5_500_000L, Color(0xFF2E7D32)),
    BAND_40M("40m", 7_000_000L, 7_300_000L, Color(0xFF00008B)),
    BAND_30M("30m", 10_100_000L, 10_150_000L, Color(0xFF137A7A)),
    BAND_20M("20m", 14_000_000L, 14_350_000L, Color(0xFFC7C41A)),
    BAND_17M("17m", 18_068_000L, 18_168_000L, Color(0xFF396925)),
    BAND_15M("15m", 21_000_000L, 21_450_000L, Color(0xFF21BE02)),
    BAND_12M("12m", 24_890_000L, 24_990_000L, Color(0xFFB8590B)),
    BAND_10M("10m", 28_000_000L, 29_700_000L, Color(0xFFB22222)),
    BAND_6M("6m", 50_000_000L, 54_000_000L, Color(0xFFFF0080)),
    BAND_2M("2m", 144_000_000L, 148_000_000L, Color(0xFF00BCD4)),
    BAND_70CM("70cm", 430_000_000L, 450_000_000L, Color(0xFF9C27B0));

    companion object {
        fun fromFrequency(freqHz: Long?): HamBand? {
            if (freqHz == null || freqHz <= 0) return null
            return entries.firstOrNull { freqHz in it.minFreqHz..it.maxFreqHz }
        }

        fun fromString(label: String?): HamBand? {
            if (label.isNullOrBlank()) return null
            val clean = label.trim().lowercase().removePrefix("band_")
            val target = if (clean.endsWith("m") || clean.endsWith("cm")) clean else "${clean}m"
            return entries.firstOrNull { it.label.equals(target, ignoreCase = true) }
        }

        val DEFAULT_BANDS = setOf("160m", "80m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m")
    }
}
