package com.team.quadcore.backend.dto

data class BusApiResponse(val response: BusResponse?)
data class BusResponse(val body: BusBody?)
data class BusBody(val items: BusItems?)
data class BusItems(val item: List<BusItem>?)

data class BusItem(
    val routeid: String?,
    val routeno: String?,
    val routetp: String?,
    val startnodenm: String?,
    val endnodenm: String?,
    val startvehicletime: String?,
    val endvehicletime: String?
)
