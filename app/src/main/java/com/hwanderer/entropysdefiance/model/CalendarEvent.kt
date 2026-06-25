// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.model

import java.time.LocalDateTime

/**
 * Model class representing a calendar event
 */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val location: String? = null,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val isAllDay: Boolean = false,
    val color: Int? = null,
    val eventId: Long? = null,
    val calendarId: Long? = null,
    // Raw provider instance times (epoch millis) for this specific occurrence. eventId is the
    // master series ID — identical for every occurrence — so these are needed to open the exact
    // instance of a recurring event the user tapped.
    val beginMillis: Long? = null,
    val endMillis: Long? = null
)