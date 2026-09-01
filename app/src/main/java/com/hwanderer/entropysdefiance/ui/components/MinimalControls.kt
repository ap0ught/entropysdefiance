// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding

/**
 * Shared "minimal" controls that give the app its typographic look: borderless inputs with a
 * single accent underline, plain-text toggles, and large centered selectable text. Used by the
 * task-creation and alignment screens so they stay visually consistent.
 */

/**
 * A borderless text input with a centered value and a single accent underline (no box).
 */
@Composable
fun UnderlinedInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accent: Color,
    textColor: Color,
    placeholderColor: Color,
    textStyle: TextStyle,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle.copy(color = textColor),
        cursorBrush = SolidColor(accent),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { innerTextField ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(color = placeholderColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    innerTextField()
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(accent.copy(alpha = 0.6f))
                )
            }
        }
    )
}

/** A plain text mode label; the active one is in the accent color with a short underline. */
@Composable
fun ModeLabel(
    text: String,
    selected: Boolean,
    accent: Color,
    mutedColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) accent else mutedColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(2.dp)
                .background(if (selected) accent else Color.Transparent)
        )
    }
}

/**
 * A minimal screen header: a back arrow on the leading edge and a light, centered title —
 * the same treatment as the alignment screen, used across the settings sub-screens.
 */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(safeTopInsets())
            .padding(top = 8.dp, bottom = 12.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/** Large, centered selectable text: the accent color when selected, muted otherwise. */
@Composable
fun SelectableTextRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp),
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Light,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        },
        textAlign = TextAlign.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 24.dp)
    )
}


/**
 * The top inset to keep content clear of, measured rather than guessed.
 *
 * `statusBarsPadding()` is not enough on its own: fullscreen mode hides the status bar, so
 * that inset collapses to zero and content slides up under the camera cutout. `safeDrawing`
 * keeps the cutout in the calculation, so hiding the bar reclaims the bar's height and
 * nothing more.
 *
 * The window draws edge to edge, so this has to be applied by each screen — and it replaces
 * the hardcoded 40dp top paddings that were previously standing in for it and falling short
 * on taller status bars.
 */
@Composable
fun safeTopInsets(): WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
