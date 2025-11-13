package com.team.quadcore.backend.model

// BusStop 문서 내에 비정규화하여 저장될 노선(Bus) 정보
data class BusLite(
    val routeId: String,
    val shortName: String,
    val routeType: String?
)
