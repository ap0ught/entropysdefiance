// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hwanderer.entropysdefiance.model.AppSettings
import kotlin.math.roundToInt

/**
 * Screen for configuring free time calculation settings
 */
@Composable
fun FreeTimeSettingsScreen(
    appSettings: AppSettings,
    onBack: () -> Unit,
    onProductivityRatioChange: (Float) -> Unit,
    onCalculationCalendarSelectionClick: () -> Unit
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
            ScreenHeader(title = "Free Time", onBack = onBack)

            val onBg = MaterialTheme.colorScheme.onBackground
            val muted = onBg.copy(alpha = 0.6f)
            val hairline = onBg.copy(alpha = 0.12f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Productive time ratio
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Productive Time Ratio",
                        style = MaterialTheme.typography.titleMedium,
                        color = onBg
                    )
                    Text(
                        text = "What share of your day is productive time? " +
                            "The rest is assumed to be sleep, meals, and personal time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(appSettings.productivityRatio * 100).roundToInt()}%",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${(appSettings.productivityRatio * 24).roundToInt()}h / day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted
                        )
                    }
                    Slider(
                        value = appSettings.productivityRatio,
                        onValueChange = onProductivityRatioChange,
                        valueRange = 0.1f..1.0f,
                        steps = 17,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("10%", style = MaterialTheme.typography.bodySmall, color = onBg.copy(alpha = 0.5f))
                        Text("100%", style = MaterialTheme.typography.bodySmall, color = onBg.copy(alpha = 0.5f))
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = hairline)

                // Calculation calendars
                val calendarCountText = if (appSettings.calculationCalendarIds.isEmpty()) {
                    "Using all display calendars"
                } else {
                    "${appSettings.calculationCalendarIds.size} calendars selected"
                }
                SettingsActionItem(
                    title = "Calculation Calendars",
                    description = calendarCountText,
                    onClick = onCalculationCalendarSelectionClick
                )

                HorizontalDivider(thickness = 0.5.dp, color = hairline)

                // How it works
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "How it works",
                        style = MaterialTheme.typography.titleMedium,
                        color = onBg
                    )
                    Text(
                        text = "Free time is calculated as:\n" +
                            "• Total hours from now until the end of the time horizon\n" +
                            "• Minus hours with calendar events (overlaps counted once)\n" +
                            "• Multiplied by your productivity ratio\n\n" +
                            "That gives the productive hours available for the current horizon.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted
                    )
                }
            }
        }
    }
}
