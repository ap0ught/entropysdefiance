// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hwanderer.entropysdefiance.model.DeviceCalendar

@Composable
fun CalendarSelectionDialog(
    availableCalendars: List<DeviceCalendar>,
    onCalendarToggle: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit = {},
    onSelectNone: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Full screen layout
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // The shared header, so this screen picks up the same insets and the same
            // typography as every other one instead of hand-rolling both.
            ScreenHeader(title = "Select Calendars", onBack = onDismiss)
            
            // Select All/None buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onSelectAll) {
                    Text("Select All")
                }
                
                TextButton(onClick = onSelectNone) {
                    Text("Select None")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calendar list
            if (availableCalendars.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No calendars found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(availableCalendars) { calendar ->
                        CalendarSelectionItem(
                            calendar = calendar,
                            onToggle = { onCalendarToggle(calendar.id) }
                        )
                        
                        if (calendar != availableCalendars.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            
            // Bottom action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
fun CalendarSelectionItem(
    calendar: DeviceCalendar,
    onToggle: () -> Unit
) {
    val calendarColor = remember(calendar.color) {
        Color(calendar.color)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Calendar color indicator
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(calendarColor)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Calendar name and account
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = calendar.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (calendar.account.isNotEmpty()) {
                Text(
                    text = calendar.account,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Selection checkbox
        Checkbox(
            checked = calendar.selected,
            onCheckedChange = { onToggle() }
        )
    }
} 