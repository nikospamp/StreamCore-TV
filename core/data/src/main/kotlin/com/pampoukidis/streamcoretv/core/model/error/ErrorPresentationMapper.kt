package com.pampoukidis.streamcoretv.core.model.error

interface ErrorPresentationMapper {
    fun map(error: AppError): ErrorModel
}