package com.kuzmix.ai.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Manual Firebase bootstrap.
 *
 * The project ships WITHOUT google-services.json and WITHOUT the
 * google-services Gradle plugin (wiring the pasted console config by
 * hand instead), so [FirebaseApp.initializeApp] is fed a [FirebaseOptions]
 * built from the Firebase console's SDK setup panel.
 */
object FirebaseConfig {
    private const val TAG = "FirebaseConfig"

    private const val API_KEY = "AIzaSyBSR8tSjzs1s4MKElnS8SxOXNldEweDSxc"
    private const val APP_ID = "1:225186319331:android:c174b9d81c5b2d50892ed6"
    private const val SENDER_ID = "225186319331"
    private const val STORAGE_BUCKET = "kuzmix-ai.firebasestorage.app"
    private const val PROJECT_ID = "kuzmix-ai"

    /**
     * OAuth Web client ID for the Google Sign-In button
     * (Firebase Console → Authentication → Sign-in method → Google →
     * Web client ID). While empty the Google button stays hidden and
     * email/password login remains fully functional.
     */
    const val GOOGLE_WEB_CLIENT_ID = "225186319331-1meq1nh7o5ojl9i4hsu5oescdcmgqqau.apps.googleusercontent.com"

    /** Idempotent — safe to call from Application.onCreate on every start. */
    fun init(context: Context) {
        if (FirebaseApp.getApps(context).isNotEmpty()) return
        val options = FirebaseOptions.Builder()
            .setApiKey(API_KEY)
            .setApplicationId(APP_ID)
            .setGcmSenderId(SENDER_ID)
            .setStorageBucket(STORAGE_BUCKET)
            .setProjectId(PROJECT_ID)
            .build()
        FirebaseApp.initializeApp(context.applicationContext, options)
        Log.i(TAG, "Firebase initialized from inline console config")
    }
}
