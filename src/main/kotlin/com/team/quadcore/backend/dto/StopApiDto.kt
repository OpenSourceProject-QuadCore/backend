package com.team.quadcore.backend.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

data class StopApiResponse(val response: StopResponse?)
data class StopResponse(val body: StopBody?)
data class StopBody(val items: StopItems?)
data class StopItems(
    @JsonProperty("item")
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
    val item: List<StopItem>?
)

data class StopItem(
    val nodeid: String?,
    val nodenm: String?,
    val nodeord: Int?,
    val updowncd: Int?
)
