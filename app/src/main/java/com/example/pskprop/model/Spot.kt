package com.example.pskprop.model

enum class SpotRole {
    SENDER,
    RECEIVER;

    fun displayName(): String = when (this) {
        SENDER -> "Sender (TX)"
        RECEIVER -> "Receiver (RX)"
    }
}

data class Spot(
    val id: String,
    val callsign: String,
    val locator: String,
    val lat: Double,
    val lon: Double,
    val partnerCallsign: String,
    val partnerLocator: String,
    val partnerLat: Double?,
    val partnerLon: Double?,
    val band: String,
    val frequencyHz: Long?,
    val snr: Int?,
    val timestampSeconds: Long,
    val mode: String,
    val kind: SpotRole,
    val dxcc: String? = null,
    val distanceKm: Double = 0.0,
    val bearingDeg: Double = 0.0
) {
    val frequencyMhz: String
        get() = frequencyHz?.let { String.format("%.3f MHz", it / 1_000_000.0) } ?: ""

    val snrDisplay: String
        get() = snr?.let { "$it dB" } ?: "N/A"

    val dotRadiusDp: Float
        get() = getDotRadiusDp(1.0f)

    fun getDotRadiusDp(scale: Float): Float {
        val base = when {
            snr == null -> 2.5f
            snr <= -20 -> 2.5f
            snr <= 0 -> 4.5f
            else -> 6.5f
        }
        return (base * scale).coerceAtLeast(1.5f)
    }
}
