package com.team.quadcore.backend.util

object GeoUtil {
    fun calculateGeohash(lat: Double, lng: Double, length: Int = 12): String {
        return "gh_${lat}_${lng}"
    }
}
