package com.pampoukidis.streamcoretv.client.clientb.data.model

internal data class ClientBHomeLaneDto(
    val laneId: String,
    val title: String,
    val caption: String,
    val template: ClientBLaneTemplateDto,
    val assets: List<ClientBContentDto>,
)
