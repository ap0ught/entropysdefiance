package com.example.entropysdefiance.ui

import android.app.Activity
import android.content.pm.LauncherApps
import android.os.Bundle
import android.widget.Toast

class ConfirmPinShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
        val request = launcherApps.getPinItemRequest(intent)
        if (request != null && request.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            if (request.accept()) {
                Toast.makeText(this, "Shortcut added to app drawer", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }
}
