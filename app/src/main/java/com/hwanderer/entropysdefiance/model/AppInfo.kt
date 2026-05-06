package com.hwanderer.entropysdefiance.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val launchIntent: String,
    val shortcutId: String? = null
) {
    val usageKey: String get() = if (shortcutId != null) "shortcut:$packageName:$shortcutId" else packageName
} 