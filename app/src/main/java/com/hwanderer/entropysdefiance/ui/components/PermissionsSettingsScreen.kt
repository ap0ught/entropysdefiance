package com.hwanderer.entropysdefiance.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.hwanderer.entropysdefiance.utils.buildDefaultLauncherIntent
import com.hwanderer.entropysdefiance.utils.isAppDefaultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun PermissionsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Permission states - reactive to permission changes
    var calendarPermissionGranted by remember { mutableStateOf(false) }
    var isDefaultLauncher by remember { mutableStateOf(false) }

    // Function to refresh permission states
    fun refreshPermissions() {
        calendarPermissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        isDefaultLauncher = isAppDefaultLauncher(context)
    }

    // Initialize permission states on first load
    LaunchedEffect(Unit) {
        refreshPermissions()
    }

    // Permission launchers
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            calendarPermissionGranted = granted
        }
    )

    // Returns from the "Home app" role dialog (or Home settings) — re-check status.
    val defaultLauncherResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { refreshPermissions() }
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            ScreenHeader(title = "Permissions", onBack = onBack)

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Intro
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "App permissions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Grant permissions to enable full functionality. You can change " +
                            "these in your device settings anytime.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    TextButton(
                        onClick = { refreshPermissions() },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "Refresh status",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Default launcher — many OEMs (e.g. Samsung One UI) never prompt to switch
                // home apps, so offer it explicitly here.
                PermissionItem(
                    title = "Default Launcher",
                    description = "Set Entropy's Defiance as your home app so it opens when you " +
                            "press Home or unlock your device.",
                    isGranted = isDefaultLauncher,
                    buttonLabel = "Set as Default Launcher",
                    onRequestPermission = {
                        defaultLauncherResult.launch(buildDefaultLauncherIntent(context))
                    }
                )

                // Calendar permission
                PermissionItem(
                    title = "Calendar Access",
                    description = "Read calendar events for free time calculation and agenda display",
                    isGranted = calendarPermissionGranted,
                    onRequestPermission = {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    buttonLabel: String = "Grant Permission"
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = onBg
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBg.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = if (isGranted) "Granted" else "Not granted",
                tint = if (isGranted) accent else onBg.copy(alpha = 0.45f)
            )
        }

        if (!isGranted) {
            TextButton(
                onClick = onRequestPermission,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(buttonLabel, color = accent, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 0.5.dp, color = onBg.copy(alpha = 0.12f))
    }
}