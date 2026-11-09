/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.playback

import com.minetallest.minefiy.innertube.pages.QueueChip

data class QueueRecommendations(
    val chips: List<QueueChip> = emptyList(),
    val loading: Boolean = false,
    val pendingChipId: String? = null,
    val failed: Boolean = false,
) {
    internal fun acceptResponse(returnedChips: List<QueueChip>, requestedChipId: String? = null): QueueRecommendations {
        val available = returnedChips.ifEmpty { chips }
        val requested = chips.firstOrNull { it.id == requestedChipId }
        val updated = if (requested == null || returnedChips.any { it.selected }) {
            available
        } else {
            available.map { chip ->
                val matchesRequest = (chip.id == requested.id && chip.title == requested.title) ||
                    chip.endpoint == requested.endpoint
                chip.copy(selected = !requested.selected && matchesRequest)
            }
        }
        return QueueRecommendations(chips = updated)
    }
}
