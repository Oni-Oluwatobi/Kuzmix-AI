package com.kuzmix.ai

import android.app.Application
import com.kuzmix.ai.data.auth.FirebaseConfig
import com.kuzmix.ai.di.AppContainer

class KuzmixApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Manual Firebase bootstrap (no google-services.json) — must run
        // before AppContainer touches FirebaseAuth/Firestore.
        FirebaseConfig.init(this)
        container = AppContainer(this)
    }
}
