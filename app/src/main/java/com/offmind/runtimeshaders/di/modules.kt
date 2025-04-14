package com.offmind.runtimeshaders.di

import com.offmind.runtimeshaders.screens.editor.NodeEditorViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val mainModules = module {
    viewModelOf(::NodeEditorViewModel)
}