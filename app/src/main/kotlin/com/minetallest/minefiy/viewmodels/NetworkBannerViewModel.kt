/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import com.minetallest.minefiy.network.NetworkBannerUiState
import com.minetallest.minefiy.network.ObserveNetworkBannerStateUseCase
import javax.inject.Inject

@HiltViewModel
class NetworkBannerViewModel
    @Inject
    constructor(
        observeNetworkBannerStateUseCase: ObserveNetworkBannerStateUseCase,
    ) : ViewModel() {
        val bannerState: StateFlow<NetworkBannerUiState> =
            observeNetworkBannerStateUseCase()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = NetworkBannerUiState.Hidden,
                )
    }
