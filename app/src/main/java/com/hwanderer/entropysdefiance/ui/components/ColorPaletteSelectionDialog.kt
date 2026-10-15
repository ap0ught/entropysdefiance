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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hwanderer.entropysdefiance.ui.theme.SYSTEM_COLOR_THEME
import com.hwanderer.entropysdefiance.ui.theme.availableColorPalettes
import com.hwanderer.entropysdefiance.ui.theme.parseHexColor
import com.hwanderer.entropysdefiance.ui.theme.resolveColorPalette

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ColorPaletteSelectionDialog(
    currentColorTheme: String,
    onColorSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCustomSelected = currentColorTheme.startsWith("#")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            ScreenHeader(title = "Color Theme", onBack = onDismiss)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Follow system (Material You) accent.
                SystemColorOption(
                    isSelected = currentColorTheme == SYSTEM_COLOR_THEME,
                    onClick = { onColorSelect(SYSTEM_COLOR_THEME) }
                )

                // Preset swatches.
                SectionLabel("Presets")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    availableColorPalettes.forEach { palette ->
                        SwatchButton(
                            color = palette.primary,
                            isSelected = currentColorTheme == palette.displayName,
                            onClick = { onColorSelect(palette.displayName) }
                        )
                    }
                }

                // Custom hex color.
                SectionLabel("Custom")
                HexColorInput(
                    currentHex = if (isCustomSelected) currentColorTheme else "",
                    isSelected = isCustomSelected,
                    onApply = onColorSelect
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun SystemColorOption(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    // Preview the live system accent (Android 12+ derives this from the wallpaper).
    val systemAccent = remember { resolveColorPalette(SYSTEM_COLOR_THEME, context).primary }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ColorCircle(color = systemAccent)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Follow system colors",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Material You accent from your wallpaper",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SwatchButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val ringColor = if (isSelected) {
        MaterialTheme.colorScheme.onBackground
    } else {
        Color.White.copy(alpha = 0.2f)
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (isSelected) 3.dp else 1.dp, ringColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (color.isLight()) Color.Black else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HexColorInput(
    currentHex: String,
    isSelected: Boolean,
    onApply: (String) -> Unit
) {
    var text by remember(currentHex) { mutableStateOf(currentHex.removePrefix("#")) }
    val normalized = "#" + text.trim().uppercase()
    val parsed = remember(text) { if (text.length in 6..8) parseHexColor(normalized) else null }
    val isValid = parsed != null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ColorCircle(color = parsed ?: MaterialTheme.colorScheme.surfaceVariant)
        OutlinedTextField(
            value = text,
            onValueChange = { new -> text = new.filter { it.isLetterOrDigit() }.take(8) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            prefix = { Text("#") },
            placeholder = { Text("RRGGBB") },
            isError = text.isNotEmpty() && !isValid,
            label = { Text("Hex color") }
        )
        Button(
            onClick = { if (isValid) onApply(normalized) },
            enabled = isValid
        ) {
            Text(if (isSelected) "Update" else "Apply")
        }
    }
}

@Composable
fun ColorCircle(color: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
    )
}

/** Quick luminance check to pick a contrasting check-mark color on a swatch. */
private fun Color.isLight(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) > 0.6f
