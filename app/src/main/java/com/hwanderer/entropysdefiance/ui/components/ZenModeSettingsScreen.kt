// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hwanderer.entropysdefiance.model.AppSettings

/**
 * Settings for Zen Mode — the single-task focus view.
 *
 * These began life as toggles in the main settings menu, which is a centred typographic list
 * of destinations with room for only very short labels. "Do Not Disturb in Zen Mode" wrapped
 * onto two lines and pushed its marker out to the side, breaking the vertical rhythm the
 * whole menu depends on. Settings that need explaining belong on a screen; the menu lists
 * places to go.
 */
@Composable
fun ZenModeSettingsScreen(
    appSettings: AppSettings,
    onToggleDndInZenMode: () -> Unit,
    onToggleDndTotalSilence: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            ScreenHeader(title = "Zen Mode", onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                SettingsSwitchItem(
                    title = "Do Not Disturb",
                    description = "Silence notifications while you focus on a task",
                    checked = appSettings.enableDndInZenMode,
                    onCheckedChange = { onToggleDndInZenMode() }
                )

                if (appSettings.enableDndInZenMode) {
                    SettingsSwitchItem(
                        title = "Total Silence",
                        description = "Also mute alarms and calls",
                        checked = appSettings.dndTotalSilence,
                        onCheckedChange = { onToggleDndTotalSilence() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your previous Do Not Disturb setting is restored when the " +
                        "session ends. Needs Do Not Disturb access, which is granted in " +
                        "Permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
