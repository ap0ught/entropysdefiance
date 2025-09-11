package com.example.entropysdefiance.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.entropysdefiance.model.AppSettings

@Composable
fun CalendarSettingsScreen(
    appSettings: AppSettings,
    onBack: () -> Unit,
    onToggleCalendarWidget: () -> Unit,
    onCalendarDaysChange: (Int) -> Unit,
    onCalendarSelectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header with back button and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 50.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Calendar Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // Calendar widget toggle
                SettingsSwitchItem(
                    title = "Show Calendar Widget",
                    description = "Display calendar events on the home screen",
                    checked = appSettings.showCalendarWidget,
                    onCheckedChange = { onToggleCalendarWidget() }
                )
                
                if (appSettings.showCalendarWidget) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Calendar selection
                    SettingsActionItem(
                        title = "Calendar Selection",
                        description = "Choose which calendars to display",
                        onClick = onCalendarSelectionClick
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
