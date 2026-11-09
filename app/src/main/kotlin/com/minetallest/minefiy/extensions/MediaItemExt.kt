/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.extensions

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE
import com.minetallest.minefiy.db.entities.Song
import com.minetallest.minefiy.innertube.models.EpisodeItem
import com.minetallest.minefiy.innertube.models.SongItem
import com.minetallest.minefiy.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.minetallest.minefiy.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_UGC
import com.minetallest.minefiy.models.MediaMetadata
import com.minetallest.minefiy.models.toMediaMetadata
import com.minetallest.minefiy.ui.utils.YTThumbQuality
import com.minetallest.minefiy.ui.utils.YtimgResizePolicy
import com.minetallest.minefiy.ui.utils.buildYTThumbnailUrl
import com.minetallest.minefiy.ui.utils.resize
import com.minetallest.minefiy.utils.NotificationArtworkSizePx
import com.minetallest.minefiy.utils.isLocalMediaId

const val ExtraIsMusicVideo = "com.minetallest.minefiy.extra.IS_MUSIC_VIDEO"
const val ExtraIsPodcast = "com.minetallest.minefiy.extra.IS_PODCAST"

val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata

private fun String?.toNotificationArtworkUri() =
    this
        ?.resize(
            width = NotificationArtworkSizePx,
            height = NotificationArtworkSizePx,
            ytimgResizePolicy = YtimgResizePolicy.PreserveOriginal,
        )?.toUri()

private fun MediaItem.Builder.setCacheKeyIfRemote(mediaId: String): MediaItem.Builder {
    if (!mediaId.isLocalMediaId()) {
        setCustomCacheKey(mediaId)
    }
    return this
}

fun Song.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(song.id)
        .setUri(song.id)
        .setCacheKeyIfRemote(song.id)
        .setTag(toMediaMetadata())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(song.title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(
                    if (song.isMusicVideo) {
                        buildYTThumbnailUrl(song.id, YTThumbQuality.HQ).toUri()
                    } else {
                        song.thumbnailUrl.toNotificationArtworkUri()
                    },
                )
                .setAlbumTitle(song.albumName)
                .setAlbumArtist(song.albumArtist)
                .setTrackNumber(song.trackNumber)
                .setDiscNumber(song.discNumber)
                .setReleaseYear(song.year)
                .setIsPlayable(true)
                .setMediaType(if (song.isPodcast) MEDIA_TYPE_PODCAST_EPISODE else MEDIA_TYPE_MUSIC)
                .setExtras(
                    Bundle().apply {
                        putBoolean(ExtraIsMusicVideo, song.isMusicVideo)
                        putBoolean(ExtraIsPodcast, song.isPodcast)
                    },
                )
                .build(),
        ).build()

fun SongItem.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(id)
        .setUri(id)
        .setCacheKeyIfRemote(id)
        .setTag(toMediaMetadata())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(
                    if (isMusicVideo()) {
                        buildYTThumbnailUrl(id, YTThumbQuality.HQ).toUri()
                    } else {
                        thumbnail.toNotificationArtworkUri()
                    },
                ).setAlbumTitle(album?.name)
                .setIsPlayable(true)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(
                    Bundle().apply {
                        putBoolean(ExtraIsMusicVideo, isMusicVideo())
                        putBoolean(ExtraIsPodcast, false)
                    },
                )
                .build(),
        ).build()

fun EpisodeItem.toMediaItem() = toMediaMetadata().toMediaItem()

fun MediaMetadata.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(id)
        .setUri(id)
        .setCacheKeyIfRemote(id)
        .setTag(this)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(
                    if (isMusicVideo) {
                        buildYTThumbnailUrl(id, YTThumbQuality.HQ).toUri()
                    } else {
                        thumbnailUrl.toNotificationArtworkUri()
                    },
                ).setAlbumTitle(album?.title)
                .setAlbumArtist(albumArtist)
                .setTrackNumber(trackNumber)
                .setDiscNumber(discNumber)
                .setReleaseYear(year)
                .setIsPlayable(true)
                .setMediaType(if (isPodcast) MEDIA_TYPE_PODCAST_EPISODE else MEDIA_TYPE_MUSIC)
                .setExtras(
                    Bundle().apply {
                        putBoolean(ExtraIsMusicVideo, isMusicVideo)
                        putBoolean(ExtraIsPodcast, isPodcast)
                    },
                )
                .build(),
        ).build()

private fun SongItem.isMusicVideo(): Boolean {
    val musicVideoType = endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
    return musicVideoType == MUSIC_VIDEO_TYPE_OMV || musicVideoType == MUSIC_VIDEO_TYPE_UGC
}
