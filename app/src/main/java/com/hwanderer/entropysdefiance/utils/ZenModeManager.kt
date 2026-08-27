// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.utils

import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
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

        private const val DND_PREFS = "zen_mode_dnd"
        private const val KEY_DND_OWNED = "dnd_owned_by_zen"
        private const val KEY_PREVIOUS_FILTER = "dnd_previous_filter"
    }
    
    private val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, ZenModeDeviceAdmin::class.java)
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Deliberately SharedPreferences and not DataStore: this is crash-recovery state that has
    // to be written synchronously at the moment DND is switched on. If the process dies a
    // millisecond later, the interruption filter is a *global system* setting and would stay
    // on with nothing left to remember it was us.
    private val dndPrefs = context.getSharedPreferences(DND_PREFS, Context.MODE_PRIVATE)
    
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
     * Whether the user has granted "Do Not Disturb access".
     *
     * Declaring ACCESS_NOTIFICATION_POLICY in the manifest is not sufficient — this is a
     * special access that can only be granted from system settings, never from a runtime
     * permission dialog.
     */
    fun isDndAccessGranted(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    /** Intent for the system screen where DND access is granted. */
    fun dndAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Turns on Do Not Disturb for the duration of a Zen Mode session.
     *
     * Two deliberate restraints:
     * - If the user already has some filter active, we leave it completely alone rather than
     *   overwriting a choice they made themselves — and we do not record ownership, so
     *   [restoreDnd] will not switch off a DND they turned on.
     * - Priority mode by default, not total silence: alarms and calls still come through
     *   unless the user explicitly asks for silence. Being unreachable should be opt-in.
     */
    fun enableDnd(totalSilence: Boolean): Boolean {
        if (!isDndAccessGranted()) {
            Log.w(TAG, "DND access not granted, cannot enable Do Not Disturb")
            return false
        }
        return try {
            val previous = notificationManager.currentInterruptionFilter
            if (previous != NotificationManager.INTERRUPTION_FILTER_ALL) {
                Log.d(TAG, "A filter is already active ($previous), leaving it untouched")
                return true
            }
            dndPrefs.edit()
                .putInt(KEY_PREVIOUS_FILTER, previous)
                .putBoolean(KEY_DND_OWNED, true)
                .commit()
            notificationManager.setInterruptionFilter(
                if (totalSilence) {
                    NotificationManager.INTERRUPTION_FILTER_NONE
                } else {
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                }
            )
            Log.d(TAG, "Do Not Disturb enabled for Zen Mode (totalSilence=$totalSilence)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable Do Not Disturb", e)
            dndPrefs.edit().remove(KEY_DND_OWNED).remove(KEY_PREVIOUS_FILTER).commit()
            false
        }
    }

    /**
     * Restores whatever filter was in place before Zen Mode switched DND on.
     *
     * A no-op unless we are the ones who turned it on. Safe — and intended — to call on a
     * cold start: Zen Mode state is never restored across process death, so an ownership flag
     * still sitting in prefs means we died mid-session and left DND stuck on.
     */
    fun restoreDnd() {
        if (!dndPrefs.getBoolean(KEY_DND_OWNED, false)) return
        val previous = dndPrefs.getInt(
            KEY_PREVIOUS_FILTER,
            NotificationManager.INTERRUPTION_FILTER_ALL
        )
        dndPrefs.edit().remove(KEY_DND_OWNED).remove(KEY_PREVIOUS_FILTER).commit()
        if (!isDndAccessGranted()) {
            Log.w(TAG, "DND access revoked; cannot restore filter $previous")
            return
        }
        try {
            notificationManager.setInterruptionFilter(previous)
            Log.d(TAG, "Restored interruption filter to $previous")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore interruption filter", e)
        }
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
