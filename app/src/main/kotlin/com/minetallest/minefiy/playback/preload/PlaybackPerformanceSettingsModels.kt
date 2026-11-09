/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.playback.preload

import com.minetallest.minefiy.constants.AudioQuality
import com.minetallest.minefiy.innertube.PlaybackAuthState

data class PlaybackPerformanceSettings(
    val lowDataModeEnabled: Boolean,
    val preloadNextSongEnabled: Boolean,
    val hasPersistedValue: Boolean,
)

data class PlaybackPreloadConfiguration(
    val enabled: Boolean,
    val quality: AudioQuality,
    val authState: PlaybackAuthState,
)
