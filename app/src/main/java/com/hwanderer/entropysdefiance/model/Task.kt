// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.model

import java.time.LocalDate

data class Task(
    val id: String,
    val title: String,
    val timeHorizon: TimeHorizon,
    val isDeadlineTask: Boolean = false,
    val deadline: LocalDate? = null,
    val doDate: LocalDate? = null
)

enum class TimeHorizon(val title: String) {
    Today("Today"),
    ThisWeek("This Week"),
    ThisMonth("This Month"),
    ThisYear("This Year"),
    ThisLife("This Life")
} 