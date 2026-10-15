// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.utils

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * True if this app is currently the system's default home (launcher). Resolving the
 * CATEGORY_HOME intent is the most reliable check across OEMs.
 */
fun isAppDefaultLauncher(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = context.packageManager.resolveActivity(
        intent,
        PackageManager.MATCH_DEFAULT_ONLY
    )
    return resolved?.activityInfo?.packageName == context.packageName
}

/**
 * Intent that lets the user make this app the default launcher. Prefers the in-context
 * ROLE_HOME system dialog (Android 10+), falling back to the Home-app settings screen for
 * OEMs/situations where the role isn't available or is already held.
 */
fun buildDefaultLauncherIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        RoleManagerApi29.buildRoleIntent(context)?.let { return it }
    }
    return Intent(Settings.ACTION_HOME_SETTINGS)
}

@RequiresApi(Build.VERSION_CODES.Q)
private object RoleManagerApi29 {
    fun buildRoleIntent(context: Context): Intent? {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        if (roleManager != null &&
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        ) {
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        }
        return null
    }
}
