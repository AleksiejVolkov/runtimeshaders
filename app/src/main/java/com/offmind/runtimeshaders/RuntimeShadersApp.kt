package com.offmind.runtimeshaders

import android.app.Application
import com.offmind.runtimeshaders.di.mainModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RuntimeShadersApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RuntimeShadersApp)
            modules(mainModules)
        }
    }
}