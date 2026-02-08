package com.clearchain.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ClearChainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize app-wide components here if needed
    }
}