package com.example.pskprop.model

enum class MapProjectionType(val label: String, val description: String) {
    AEQD("Great Circle (AEQD)", "Azimuthal Equidistant centered on Home Grid"),
    ROBINSON("Robinson", "Pseudo-cylindrical projection centered on Home Longitude"),
    MERCATOR("Mercator", "Conformal cylindrical projection")
}
