/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.extensions

import com.minetallest.minefiy.innertube.models.AlbumItem
import com.minetallest.minefiy.innertube.models.ArtistItem
import com.minetallest.minefiy.innertube.models.EpisodeItem
import com.minetallest.minefiy.innertube.models.PlaylistItem
import com.minetallest.minefiy.innertube.models.PodcastItem
import com.minetallest.minefiy.innertube.models.SongItem
import com.minetallest.minefiy.innertube.models.YTItem
import com.minetallest.minefiy.innertube.pages.BrowseResult

fun <T : YTItem> List<T>.filterBlockedArtists(blockedArtistIds: Set<String>): List<T> {
    if (blockedArtistIds.isEmpty()) return this

    return filter { item ->
        when (item) {
            is ArtistItem -> item.id !in blockedArtistIds
            is SongItem -> item.artists.none { it.id in blockedArtistIds }
            is AlbumItem -> item.artists.orEmpty().none { it.id in blockedArtistIds }
            is PlaylistItem -> item.author?.id !in blockedArtistIds
            is PodcastItem -> item.author?.id !in blockedArtistIds
            is EpisodeItem -> item.podcast?.id !in blockedArtistIds
        }
    }
}

fun BrowseResult.filterBlockedArtists(blockedArtistIds: Set<String>): BrowseResult {
    if (blockedArtistIds.isEmpty()) return this

    return copy(
        items =
            items.mapNotNull { section ->
                section.copy(
                    items =
                        section.items
                            .filterBlockedArtists(blockedArtistIds)
                            .ifEmpty { return@mapNotNull null },
                )
            },
    )
}
