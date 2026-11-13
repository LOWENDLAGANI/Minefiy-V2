/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.lockdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The lock the UI renders.
 *
 * It checks once on creation, because that is when a lockdown has to be noticed,
 * and exposes a retry so the locked screen is not a dead end: a device that was
 * blocked by mistake can be let in by the owner and released with one tap.
 */
@HiltViewModel
class LockdownViewModel
    @Inject
    constructor(
        private val repository: LockdownRepository,
    ) : ViewModel() {
        val policy = repository.policy

        private val _checking = MutableStateFlow(false)
        val checking = _checking.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _checking.value = true
                try {
                    repository.refresh()
                } finally {
                    _checking.value = false
                }
            }
        }
    }

/**
 * The panel needs the repository, not this screen's lock state: after a save it
 * applies the policy it just wrote, so the device answers immediately instead of
 * waiting for its next read.
 */
@HiltViewModel
class AdminPanelViewModel
    @Inject
    constructor(
        val repository: LockdownRepository,
    ) : ViewModel()
