package com.example.pskprop.model

enum class QueryDirection(val label: String) {
    BOTH("Tx & Rx (Either)"),
    TX_ONLY("Sent by (TX)"),
    RX_ONLY("Heard by (RX)")
}

enum class DotSizeOption(val label: String, val scale: Float) {
    TINY("Tiny (0.35×)", 0.35f),
    SMALL("Small (0.50×)", 0.50f),
    MEDIUM("Medium (0.70×)", 0.70f),
    LARGE("Large (1.0×)", 1.0f)
}

data class PropConfig(
    val homeLocator: String = "KP20",
    val homeLat: Double = 60.1708,
    val homeLon: Double = 24.9375,
    val callsignFilter: String = "",
    val queryDirection: QueryDirection = QueryDirection.BOTH,
    val radiusKm: Double = 400.0,
    val ageMinutes: Int = 15,
    val selectedBands: Set<String> = HamBand.DEFAULT_BANDS,
    val selectedMode: String = "All",
    val mapProjection: MapProjectionType = MapProjectionType.AEQD,
    val showGrayline: Boolean = true,
    val showRings: Boolean = true,
    val autoRefreshSeconds: Int = 60,
    val darkMapTheme: Boolean = true,
    val dotScale: Float = 0.50f
)
