package com.team.quadcore.backend.model

import java.util.*

// Firestore 'bus_stops' 컬렉션 (버스 정류장 정보)

data class BusStop(
    val id: String, // nodeId
    val name: String,
    val code: String?, // nodeNo from StationItem
    val lat: Double,
    val lng: Double,
    val geohash: String,
    val cityCode: String,
    val busesLite: List<BusLite> = emptyList(),
    val updatedAt: Date
)
