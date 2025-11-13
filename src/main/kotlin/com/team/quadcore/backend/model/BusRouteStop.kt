package com.team.quadcore.backend.model

import java.util.*

// Firestore 'bus_route_stops' 컬렉션 (노선-정류장 매핑)
data class BusRouteStop(
    val routeId: String,
    val stopId: String,
    val sequence: Int, // nodeord
    val direction: String?, // updownflag
    val cityCode: String,
    val updatedAt: Date
)
