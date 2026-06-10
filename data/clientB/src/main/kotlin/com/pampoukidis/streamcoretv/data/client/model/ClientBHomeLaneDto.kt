package com.pampoukidis.streamcoretv.data.client.model

internal data class ClientBHomeLaneDto(
    val laneId: String,
    val title: String,
    val caption: String,
    val template: ClientBLaneTemplateDto,
    val assets: List<ClientBContentDto>,
)
