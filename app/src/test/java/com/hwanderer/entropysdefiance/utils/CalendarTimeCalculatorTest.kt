// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.utils

import com.hwanderer.entropysdefiance.model.CalendarEvent
import com.hwanderer.entropysdefiance.model.TimeHorizon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class CalendarTimeCalculatorTest {

    // Midday, so "Today" always has a meaningful number of hours left in it and the result
    // does not depend on when the suite happens to run.
    private val now: LocalDateTime = LocalDateTime.of(2026, 9, 1, 12, 0)

    private fun event(
        id: Long,
        start: LocalDateTime,
        end: LocalDateTime,
        allDay: Boolean = false
    ) = CalendarEvent(
        id = id,
        title = "Event $id",
        startTime = start,
        endTime = end,
        isAllDay = allDay
    )

    @Test
    fun `all-day event spanning the horizon leaves the day free`() {
        // Given a fortnight-long all-day event covering today — a trip, a visitor, a holiday
        val events = listOf(
            event(1, now.minusDays(10), now.plusDays(10), allDay = true)
        )

        // When free time for today is calculated
        val metric = CalendarTimeCalculator.calculateFreeTime(
            events = events,
            timeHorizon = TimeHorizon.Today,
            productivityRatio = 1.0f,
            now = now
        )

        // Then it consumes none of the day. Counting it would block every remaining hour and
        // pin the readout to "0h available" for as long as the event lasts.
        assertEquals(0.0, metric.scheduledHours, 0.001)
        assertTrue("expected free hours to remain", metric.productiveHoursAvailable > 0.0)
    }

    @Test
    fun `timed events still count against free time`() {
        // Given a two-hour meeting this afternoon
        val events = listOf(
            event(1, now.plusHours(1), now.plusHours(3))
        )

        // When free time for today is calculated
        val metric = CalendarTimeCalculator.calculateFreeTime(
            events = events,
            timeHorizon = TimeHorizon.Today,
            productivityRatio = 1.0f,
            now = now
        )

        // Then those two hours are busy
        assertEquals(2.0, metric.scheduledHours, 0.001)
    }

    @Test
    fun `an all-day event does not mask a timed one on the same day`() {
        // Given a birthday marked all-day and a real appointment on the same day
        val events = listOf(
            event(1, now.minusHours(12), now.plusHours(12), allDay = true),
            event(2, now.plusHours(2), now.plusHours(4))
        )

        // When free time for today is calculated
        val metric = CalendarTimeCalculator.calculateFreeTime(
            events = events,
            timeHorizon = TimeHorizon.Today,
            productivityRatio = 1.0f,
            now = now
        )

        // Then only the appointment counts
        assertEquals(2.0, metric.scheduledHours, 0.001)
    }
}
