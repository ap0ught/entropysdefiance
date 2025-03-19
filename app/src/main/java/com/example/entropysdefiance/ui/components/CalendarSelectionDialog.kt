package com.example.entropysdefiance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.entropysdefiance.model.DeviceCalendar

@Composable
fun CalendarSelectionDialog(
    availableCalendars: List<DeviceCalendar>,
    onCalendarToggle: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit = {},
    onSelectNone: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Calendars",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                
                if (availableCalendars.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
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
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(availableCalendars) { calendar ->
                            CalendarSelectionItem(
                                calendar = calendar,
                                onToggle = { onCalendarToggle(calendar.id) }
                            )
                            
                            if (calendar != availableCalendars.last()) {
                                Divider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
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
                fontWeight = FontWeight.SemiBold,
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