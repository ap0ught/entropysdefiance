// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hwanderer.entropysdefiance.model.AppSettings

@Composable
fun AppearanceSettingsScreen(
    appSettings: AppSettings,
    onBack: () -> Unit,
    onColorPaletteClick: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onWallpaperDimChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            ScreenHeader(title = "Appearance", onBack = onBack)

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // Color palette selection
                SettingsActionItem(
                    title = "Color Theme",
                    description = "Current theme: ${appSettings.colorTheme}",
                    onClick = onColorPaletteClick
                )

                SettingsSliderItem(
                    title = "Wallpaper Dim",
                    description = "How far the wallpaper is darkened behind the launcher",
                    value = appSettings.wallpaperDim,
                    onValueChange = onWallpaperDimChange,
                    valueRange = 0f..0.95f,
                    // 5% steps: fine enough to find a comfortable point, coarse enough that
                    // the slider settles rather than drifting.
                    steps = 18,
                    valueText = "${(appSettings.wallpaperDim * 100).toInt()}%"
                )

                SettingsSwitchItem(
                    title = "Fullscreen",
                    description = "Hide the status bar; swipe down from the top edge to see it",
                    checked = appSettings.fullscreenMode,
                    onCheckedChange = { onToggleFullscreen() }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
