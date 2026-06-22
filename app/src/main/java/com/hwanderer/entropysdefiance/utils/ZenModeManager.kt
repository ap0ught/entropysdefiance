// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Manager for Zen Mode functionality including phone locking
 */
class ZenModeManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ZenModeManager"
        private const val DEVICE_ADMIN_REQUEST_CODE = 1001
    }
    
    private val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, ZenModeDeviceAdmin::class.java)
    
    /**
     * Check if device admin is enabled for this app
     */
    fun isDeviceAdminEnabled(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }
    
    /**
     * Request device admin permissions
     */
    fun requestDeviceAdminPermission(activity: ComponentActivity, launcher: ActivityResultLauncher<Intent>) {
        if (!isDeviceAdminEnabled()) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                    "This allows Zen Mode to lock your phone to help you focus on one task at a time.")
            }
            launcher.launch(intent)
        }
    }
    
    /**
     * Lock the device screen
     */
    fun lockDevice(): Boolean {
        return if (isDeviceAdminEnabled()) {
            try {
                devicePolicyManager.lockNow()
                Log.d(TAG, "Device locked for Zen Mode")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to lock device", e)
                false
            }
        } else {
            Log.w(TAG, "Device admin not enabled, cannot lock device")
            false
        }
    }
    
    /**
     * Check if the app can lock the device
     */
    fun canLockDevice(): Boolean {
        return isDeviceAdminEnabled()
    }
    
    /**
     * Get a user-friendly explanation of what device admin does
     */
    fun getDeviceAdminExplanation(): String {
        return "To provide the full Zen Mode experience, this app needs permission to lock your phone. " +
                "This ensures you can focus on one task without distractions from other apps. " +
                "You can revoke this permission at any time in your device settings."
    }
}
