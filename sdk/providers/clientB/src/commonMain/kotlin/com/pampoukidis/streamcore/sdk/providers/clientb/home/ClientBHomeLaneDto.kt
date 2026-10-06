package com.pampoukidis.streamcore.sdk.providers.clientb.home

import com.pampoukidis.streamcore.sdk.providers.clientb.catalog.ClientBContentDto

internal data class ClientBHomeLaneDto(
    val laneId: String,
    val title: String,
    val caption: String,
    val template: ClientBLaneTemplateDto,
    val assets: List<ClientBContentDto>,
)
