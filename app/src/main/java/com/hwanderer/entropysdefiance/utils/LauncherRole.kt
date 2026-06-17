package com.hwanderer.entropysdefiance.utils

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

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
    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
    if (roleManager != null &&
        roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
        !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
    ) {
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }
    return Intent(Settings.ACTION_HOME_SETTINGS)
}
