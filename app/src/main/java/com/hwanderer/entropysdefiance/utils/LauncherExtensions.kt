// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher

/**
 * Safely launches an ActivityResultLauncher, catching ActivityNotFoundException or other
 * exceptions if no Activity on the device can handle the contract/intent (e.g. OPEN_DOCUMENT_TREE).
 */
fun <I> ActivityResultLauncher<I>.launchSafe(
    input: I,
    context: Context? = null,
    errorMessage: String? = null,
    onActivityNotFound: (() -> Unit)? = null
): Boolean {
    return try {
        this.launch(input)
        true
    } catch (e: ActivityNotFoundException) {
        runCatching { Log.e("ActivityResultLauncher", "No Activity found to handle intent: ${e.message}", e) }
        if (context != null && errorMessage != null) {
            runCatching { Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show() }
        }
        onActivityNotFound?.invoke()
        false
    } catch (e: Exception) {
        runCatching { Log.e("ActivityResultLauncher", "Error launching activity result contract: ${e.message}", e) }
        if (context != null) {
            runCatching { Toast.makeText(context, "Could not open requested action: ${e.message}", Toast.LENGTH_LONG).show() }
        }
        false
    }
}
