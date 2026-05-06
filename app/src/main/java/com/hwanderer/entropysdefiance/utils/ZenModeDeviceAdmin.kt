package com.hwanderer.entropysdefiance.utils

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device Admin Receiver for Zen Mode
 * This allows the app to lock the phone during Zen Mode sessions
 */
class ZenModeDeviceAdmin : DeviceAdminReceiver() {
    
    companion object {
        private const val TAG = "ZenModeDeviceAdmin"
    }
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin enabled for Zen Mode")
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin disabled for Zen Mode")
    }
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling device admin will prevent Zen Mode from locking your phone"
    }
}
