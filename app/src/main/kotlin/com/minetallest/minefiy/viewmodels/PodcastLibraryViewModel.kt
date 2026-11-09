/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.viewmodels

import androidx.lifecycle.viewModelScope
import com.google.common.collect.ImmutableList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import com.minetallest.minefiy.R
import com.minetallest.minefiy.library.LibrarySyncTarget
import com.minetallest.minefiy.library.RefreshLibraryUseCase
import com.minetallest.minefiy.podcast.LoadPodcastUseCase
import com.minetallest.minefiy.podcast.ObserveSavedPodcastsUseCase
import com.minetallest.minefiy.podcast.PodcastLibraryAction
import com.minetallest.minefiy.podcast.PodcastLibraryEvent
import com.minetallest.minefiy.podcast.PodcastLibraryScreenState
import com.minetallest.minefiy.podcast.PodcastPlaybackRequest
import com.minetallest.minefiy.podcast.TogglePodcastSaveUseCase
import com.minetallest.minefiy.utils.LibraryLoginRequiredException
import com.minetallest.minefiy.utils.LibrarySyncDisabledException
import com.minetallest.minefiy.utils.reportException
import javax.inject.Inject

@HiltViewModel
class PodcastLibraryViewModel
    @Inject
    constructor(
        observeSavedPodcasts: ObserveSavedPodcastsUseCase,
        refreshLibrary: RefreshLibraryUseCase,
        private val loadPodcast: LoadPodcastUseCase,
        private val setPodcastSaved: TogglePodcastSaveUseCase,
    ) : LibraryRefreshViewModel(refreshLibrary) {
        private val _screenState = MutableStateFlow<PodcastLibraryScreenState>(PodcastLibraryScreenState.Loading)
        val screenState = _screenState.asStateFlow()
        private val eventChannel = Channel<PodcastLibraryEvent>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()
        private var playJob: Job? = null
        private val removeJobs = mutableMapOf<String, Job>()

        init {
            viewModelScope.launch {
                try {
                    observeSavedPodcasts().collect { uiState ->
                        _screenState.value =
                            if (uiState.podcasts.isEmpty()) {
                                PodcastLibraryScreenState.Empty
                            } else {
                                PodcastLibraryScreenState.Success(uiState)
                            }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    reportException(error)
                    _screenState.value = PodcastLibraryScreenState.Error(R.string.error_unknown)
                }
            }
            sync()
        }

        fun onAction(action: PodcastLibraryAction) {
            when (action) {
                PodcastLibraryAction.Refresh -> sync()
                is PodcastLibraryAction.PlayPodcast -> playPodcast(action.browseId)
                is PodcastLibraryAction.RemovePodcast -> removePodcast(action.browseId)
            }
        }

        private fun sync() {
            refreshLibrary(LibrarySyncTarget.Podcasts)
        }

        private fun playPodcast(browseId: String) {
            playJob?.cancel()
            playJob =
                viewModelScope.launch {
                    loadPodcast(browseId)
                        .onSuccess { result ->
                            val episodes = result.uiState.episodes
                            if (episodes.isEmpty()) {
                                eventChannel.send(PodcastLibraryEvent.ShowMessage(R.string.podcast_has_no_episodes))
                            } else {
                                eventChannel.send(
                                    PodcastLibraryEvent.Play(
                                        PodcastPlaybackRequest(
                                            title = result.uiState.title,
                                            items = ImmutableList.copyOf(episodes.map { episode -> episode.playbackMetadata }),
                                            startIndex = 0,
                                        ),
                                    ),
                                )
                            }
                        }.onFailure { throwable ->
                            reportException(throwable)
                            eventChannel.send(PodcastLibraryEvent.ShowMessage(R.string.error_unknown))
                        }
                }
        }

        private fun removePodcast(browseId: String) {
            if (removeJobs[browseId]?.isActive == true) return
            removeJobs[browseId] =
                viewModelScope.launch {
                    try {
                        setPodcastSaved(browseId, false).onFailure { throwable ->
                            reportException(throwable)
                            eventChannel.send(PodcastLibraryEvent.ShowMessage(throwable.messageResId()))
                        }
                    } finally {
                        removeJobs.remove(browseId)
                    }
                }
        }

        private fun Throwable.messageResId(): Int =
            when (this) {
                is LibraryLoginRequiredException -> R.string.not_logged_in_youtube
                is LibrarySyncDisabledException -> R.string.sync_disabled
                else -> R.string.error_unknown
            }
    }
