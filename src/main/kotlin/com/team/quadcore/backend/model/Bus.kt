package com.team.quadcore.backend.model

import java.util.*

// Firestore 'buses' 컬렉션 (버스 노선 정보)

data class Bus(
    val id: String, // routeId
    val shortName: String, // routeno
    val routeType: String?, // routetp
    val startStopName: String?,
    val endStopName: String?,
    val firstBusTime: String?,
    val lastBusTime: String?,
    val weekdayInterval: String? = null,
    val satInterval: String? = null,
    val sunInterval: String? = null,
    val cityCode: String,
    val stopsLite: List<BusStopLite> = emptyList(),
    val updatedAt: Date,
    val active: Boolean = true
)
