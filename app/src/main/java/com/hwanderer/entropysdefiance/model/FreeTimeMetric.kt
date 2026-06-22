// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.model

/**
 * Represents the calculated free time metric for a given time horizon
 */
data class FreeTimeMetric(
    val totalHoursRemaining: Double,      // Hours from now to horizon end (24h basis)
    val scheduledHours: Double,           // Non-overlapping event hours from now to end
    val freeHours24h: Double,             // totalHours - scheduledHours (24h basis)
    val productiveHoursAvailable: Double  // freeHours24h × productivityRatio
)
