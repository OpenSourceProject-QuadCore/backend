package com.team.quadcore.backend.model

// Bus 문서 내에 비정규화하여 저장될 정류장 정보
data class BusStopLite(
    val stopId: String,
    val sequence: Int
)
