package com.pampoukidis.streamcoretv.data.client.model

internal data class ClientAHomeRowDto(
    val rowId: String,
    val heading: String,
    val subheading: String,
    val presentation: ClientARowPresentationDto,
    val items: List<ClientAContentDto>,
)
