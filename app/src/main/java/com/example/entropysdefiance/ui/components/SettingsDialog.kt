package com.example.entropysdefiance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.entropysdefiance.model.AppSettings
import com.example.entropysdefiance.model.DeviceCalendar

@Composable
fun SettingsDialog(
    appSettings: AppSettings,
    onDismiss: () -> Unit,
    onToggleCalendarWidget: () -> Unit,
    onCalendarDaysChange: (Int) -> Unit,
    onToggleDarkTheme: () -> Unit,
    onToggleDoubleTapDialer: () -> Unit,
    onCalendarSelectionClick: () -> Unit,
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
            // Header with title and close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 50.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            
            // Main content in a scrollable column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // Calendar Settings Section
                SettingsSectionHeader(title = "Calendar", icon = Icons.Default.Settings)
                
                // Toggle for showing calendar
                SettingsSwitchItem(
                    title = "Show Calendar Widget",
                    description = "Display calendar events on the home screen",
                    checked = appSettings.showCalendarWidget,
                    onCheckedChange = { onToggleCalendarWidget() }
                )
                
                if (appSettings.showCalendarWidget) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Calendar days slider
                    SettingsSliderItem(
                        title = "Days to Show",
                        description = "Number of days to look ahead for events",
                        value = appSettings.calendarDaysToShow.toFloat(),
                        onValueChange = { onCalendarDaysChange(it.toInt()) },
                        valueRange = 1f..30f,
                        steps = 29,
                        valueText = "${appSettings.calendarDaysToShow} days"
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Calendar selection
                    SettingsActionItem(
                        title = "Calendar Selection",
                        description = "Choose which calendars to display",
                        onClick = onCalendarSelectionClick
                    )
                }
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                // Appearance Settings Section
                SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Settings)
                
                // Toggle for dark theme
                SettingsSwitchItem(
                    title = "Dark Theme",
                    description = "Use dark theme for the launcher",
                    checked = appSettings.useDarkTheme,
                    onCheckedChange = { onToggleDarkTheme() }
                )
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                // Gesture Settings Section
                SettingsSectionHeader(title = "Gestures", icon = Icons.Default.Settings)
                
                // Toggle for double tap dialer
                SettingsSwitchItem(
                    title = "Double Tap for Dialer",
                    description = "Double tap the home screen to open the phone app",
                    checked = appSettings.enableDoubleTapForDialer,
                    onCheckedChange = { onToggleDoubleTapDialer() }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() }
        )
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
} 