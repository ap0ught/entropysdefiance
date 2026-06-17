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
fun BackupSettingsScreen(
    appSettings: AppSettings,
    onBack: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
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
            ScreenHeader(title = "Backup & Import", onBack = onBack)

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // Export tasks
                SettingsActionItem(
                    title = "Export Tasks",
                    description = "Export your tasks to a CSV file",
                    onClick = onExportClick
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Import tasks
                SettingsActionItem(
                    title = "Import Tasks",
                    description = "Import tasks from a CSV file",
                    onClick = onImportClick
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
