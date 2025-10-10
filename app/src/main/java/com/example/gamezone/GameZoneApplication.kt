package com.example.gamezone

import android.app.Application
import com.example.gamezone.ui.helpers.CloudinaryUploader

class GameZoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CloudinaryUploader.initialize(this)
    }
}