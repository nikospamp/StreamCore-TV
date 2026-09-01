package com.pampoukidis.streamcoretv.feature.profiles.domain

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val profilesDomainModule = module {
    factoryOf(::CreateProfileUseCase)
    factoryOf(::DeleteProfileUseCase)
    factoryOf(::LoadProfileEditorOptionsUseCase)
    factoryOf(::LoadProfilesUseCase)
    factoryOf(::SelectProfileUseCase)
    factoryOf(::UpdateProfileUseCase)
    factoryOf(::ValidateProfileDraftUseCase)
}
