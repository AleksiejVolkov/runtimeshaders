package com.offmind.runtimeshaders.di

import com.offmind.runtimeshaders.screens.editor.NodeEditorViewModel
import com.offmind.runtimeshaders.screens.editor.usecase.NodesToCodeUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val mainModules = module {
    factoryOf(::NodesToCodeUseCase)
    viewModelOf(::NodeEditorViewModel)
}