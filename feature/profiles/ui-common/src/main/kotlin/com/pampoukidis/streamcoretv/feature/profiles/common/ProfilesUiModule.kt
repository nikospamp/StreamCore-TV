package com.pampoukidis.streamcoretv.feature.profiles.common

import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorViewModel
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profilesUiModule = module {
    viewModelOf(::ProfileEditorViewModel)
    viewModelOf(::ProfilesViewModel)
}
