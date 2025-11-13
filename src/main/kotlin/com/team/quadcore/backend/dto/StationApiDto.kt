package com.team.quadcore.backend.dto

data class StationListApiResponse(val response: StationListResponse?)
data class StationListResponse(val body: StationListBody?)
data class StationListBody(val items: StationListItems?)
data class StationListItems(val item: List<StationItem>?)

data class StationItem(
    val nodeid: String?,
    val nodenm: String?,
    val nodenno: String?,
    val gpslati: Double?,
    val gpslong: Double?
)
