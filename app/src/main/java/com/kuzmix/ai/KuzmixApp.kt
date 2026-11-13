package com.kuzmix.ai

import android.app.Application
import com.kuzmix.ai.di.AppContainer

class KuzmixApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
