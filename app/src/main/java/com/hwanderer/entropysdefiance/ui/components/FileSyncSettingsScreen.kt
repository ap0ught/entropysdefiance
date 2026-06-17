package com.hwanderer.entropysdefiance.ui.components

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hwanderer.entropysdefiance.model.AppSettings

@Composable
fun FileSyncSettingsScreen(
    appSettings: AppSettings,
    onBack: () -> Unit,
    onUpdateFilePath: (String) -> Unit,
    onSetupFileSync: () -> Unit,
    onSyncFromFile: () -> Unit,
    onSyncToFile: () -> Unit,
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
            ScreenHeader(title = "File Sync", onBack = onBack)

            val onBg = MaterialTheme.colorScheme.onBackground
            val accent = MaterialTheme.colorScheme.primary
            val muted = onBg.copy(alpha = 0.6f)
            val hairline = onBg.copy(alpha = 0.12f)
            val configured = appSettings.taskFilePath.isNotEmpty()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Intro
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sync tasks with an external file",
                        style = MaterialTheme.typography.titleMedium,
                        color = onBg
                    )
                    Text(
                        text = "Configure a JSON file location to sync your tasks — edit them on a " +
                            "laptop and sync across devices with tools like Syncthing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = hairline)

                // Setup / location
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (configured) "File sync location" else "Set up file sync",
                        style = MaterialTheme.typography.titleMedium,
                        color = onBg
                    )

                    if (!configured) {
                        Text(
                            text = "Not set up yet. Choose a folder where tasks.json will be created " +
                                "and kept in sync with your tasks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = muted
                        )
                        TextButton(
                            onClick = onSetupFileSync,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Choose folder…", color = accent, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (appSettings.taskFilePath.startsWith("content://")) {
                                    val uri = android.net.Uri.parse(appSettings.taskFilePath)
                                    val path = uri.path
                                    if (path != null && path.contains(":")) {
                                        val parts = path.split(":")
                                        if (parts.size > 1) "/storage/emulated/0/" + parts[1] else "Selected location"
                                    } else {
                                        "Selected location"
                                    }
                                } else {
                                    appSettings.taskFilePath
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = muted,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        TextButton(
                            onClick = onSetupFileSync,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Change folder", color = accent, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                if (configured) {
                    HorizontalDivider(thickness = 0.5.dp, color = hairline)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Manual sync",
                            style = MaterialTheme.typography.titleMedium,
                            color = onBg
                        )
                        Text(
                            text = "Tasks sync automatically when you add, edit, or complete them — " +
                                "use these only if needed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = muted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            TextButton(onClick = onSyncFromFile, contentPadding = PaddingValues(0.dp)) {
                                Text("Load from file", color = accent, fontWeight = FontWeight.Medium)
                            }
                            TextButton(onClick = onSyncToFile, contentPadding = PaddingValues(0.dp)) {
                                Text("Save to file", color = accent, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
