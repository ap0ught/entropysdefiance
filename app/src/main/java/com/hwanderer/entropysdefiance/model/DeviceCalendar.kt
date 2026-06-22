// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.model

/**
 * Represents a calendar on the device
 */
data class DeviceCalendar(
    val id: Long,
    val name: String,
    val color: Int,
    val account: String,
    val owner: String,
    val visible: Boolean,
    var selected: Boolean // Whether this calendar is selected for display
) 