/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.domain.player

import kotlinx.coroutines.flow.map
import com.minetallest.minefiy.playback.QueueRepository
import com.minetallest.minefiy.ui.player.QueueChipUi
import com.minetallest.minefiy.ui.player.QueueChips
import com.minetallest.minefiy.ui.player.QueueEntries
import com.minetallest.minefiy.ui.player.QueueEntryUi
import com.minetallest.minefiy.ui.player.QueueUiModel
import com.minetallest.minefiy.utils.makeTimeString
import javax.inject.Inject

class ObserveQueueUseCase @Inject constructor(private val repository: QueueRepository) {
    operator fun invoke() = repository.observe().map { snapshot ->
        snapshot?.let { (playback, extra, locked, autoplay, available) ->
            QueueUiModel(
                title = extra.title,
                entries = QueueEntries(playback.entries.map { entry ->
                    QueueEntryUi(entry.id, entry.metadata.title, entry.metadata.artists.joinToString { it.name },
                        entry.metadata.thumbnailUrl, makeTimeString(entry.metadata.duration.coerceAtLeast(0) * 1000L),
                        entry.current, entry.recommended)
                }),
                duration = makeTimeString(playback.entries.sumOf { it.metadata.duration.coerceAtLeast(0).toLong() } * 1000L),
                currentId = playback.entries.firstOrNull { it.current }?.id,
                playing = playback.playing, liked = extra.liked, shuffle = playback.shuffle, repeatMode = playback.repeat,
                locked = locked || extra.guest, guest = extra.guest, autoplay = autoplay, autoplayAvailable = available,
                canControlPlayback = extra.canControlPlayback,
                episodeQueue = playback.entries.firstOrNull { it.current }?.metadata?.isPodcast == true,
                loadingRecommendations = autoplay && (extra.loading || extra.recommendations.loading),
                chips = QueueChips(if (autoplay && available) extra.recommendations.chips.map { QueueChipUi(it.id, it.title, it.selected) } else emptyList()),
                recommendationsFailed = autoplay && extra.recommendations.failed,
            )
        }
    }
}
