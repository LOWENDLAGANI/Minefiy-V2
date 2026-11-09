/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.playback.queues

import androidx.media3.common.MediaItem
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import com.minetallest.minefiy.db.entities.AlbumWithSongs
import com.minetallest.minefiy.extensions.toMediaItem
import com.minetallest.minefiy.innertube.YouTube
import com.minetallest.minefiy.innertube.models.WatchEndpoint
import com.minetallest.minefiy.models.MediaMetadata
import com.minetallest.minefiy.playback.withQueueEntry
import com.minetallest.minefiy.innertube.pages.QueueChip

class LocalAlbumRadio(
    internal val albumWithSongs: AlbumWithSongs,
    internal val startIndex: Int = 0,
) : Queue {
    override val preloadItem: MediaMetadata? = null
    override val recommendationOnlyContinuation: Boolean = true
    override var queueChips: List<QueueChip> = emptyList()
        private set

    private lateinit var playlistId: String
    private val endpoint: WatchEndpoint
        get() =
            WatchEndpoint(
                playlistId = playlistId,
                params = "wAEB",
            )

    private val localOnly = albumWithSongs.album.isLocal || albumWithSongs.songs.all { it.song.isLocal }
    private val initialSongIds = albumWithSongs.songs.map { it.id }.toSet()
    private var continuation: String? = null
    private var firstTimeLoaded: Boolean = false

    override suspend fun getInitialStatus(): Queue.Status =
        withContext(IO) {
            Queue.Status(
                title = albumWithSongs.album.title,
                items = albumWithSongs.songs.map { it.toMediaItem() },
                mediaItemIndex = startIndex,
            )
        }

    override fun hasNextPage(): Boolean = !localOnly && (!firstTimeLoaded || continuation != null)

    override suspend fun nextPage(): List<MediaItem> =
        withContext(IO) {
            if (localOnly) return@withContext emptyList()
            if (!firstTimeLoaded) {
                playlistId =
                    YouTube
                        .album(albumWithSongs.album.id)
                        .getOrThrow()
                        .album.playlistId
                val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
                queueChips = nextResult.queueChips
                continuation = nextResult.continuation
                firstTimeLoaded = true
                return@withContext nextResult.items
                    .filterNot { it.id in initialSongIds }
                    .map { it.toMediaItem().withQueueEntry(recommended = true) }
            }
            val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
            if (nextResult.queueChips.isNotEmpty()) queueChips = nextResult.queueChips
            continuation = nextResult.continuation
            nextResult.items.map { it.toMediaItem().withQueueEntry(recommended = true) }
        }
}
