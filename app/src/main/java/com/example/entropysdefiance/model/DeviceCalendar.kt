package com.example.entropysdefiance.model

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