// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hwanderer.entropysdefiance.model.Task
import com.hwanderer.entropysdefiance.model.TimeHorizon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AlignmentViewModel(application: Application) : AndroidViewModel(application) {
    // Whether alignment mode is active
    private val _isAlignmentActive = MutableStateFlow(false)
    val isAlignmentActive = _isAlignmentActive.asStateFlow()
    
    // The current time horizon in the alignment process
    private val _currentAlignmentHorizon = MutableStateFlow(TimeHorizon.ThisLife)
    val currentAlignmentHorizon = _currentAlignmentHorizon.asStateFlow()
    
    // Method to start the alignment process
    fun startAlignment() {
        _isAlignmentActive.value = true
        _currentAlignmentHorizon.value = TimeHorizon.ThisLife // Start from the largest time horizon
    }
    
    // Method to move to the next time horizon (smaller)
    fun moveToNextHorizon(): Boolean {
        val currentIndex = _currentAlignmentHorizon.value.ordinal
        
        // If we're at the smallest time horizon (Today), finish alignment
        if (currentIndex == 0) {
            _isAlignmentActive.value = false
            return false
        }
        
        // Move to the next smaller time horizon
        val horizons = TimeHorizon.entries
        _currentAlignmentHorizon.value = horizons[currentIndex - 1]
        return true
    }
    
    // Method to cancel alignment
    fun cancelAlignment() {
        _isAlignmentActive.value = false
    }
} 