/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.browse

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.minetallest.minefiy.innertube.YouTube
import com.minetallest.minefiy.innertube.models.PODCAST_LIBRARY_BROWSE_ID
import com.minetallest.minefiy.innertube.models.YTItem
import javax.inject.Inject
import javax.inject.Singleton

data class BrowsePage(
    val title: String?,
    val items: List<YTItem>,
    val continuation: String?,
)

@Singleton
class BrowseRepository
    @Inject
    constructor() {
        suspend fun load(browseId: String): Result<BrowsePage> =
            withContext(Dispatchers.IO) {
                val result =
                    if (browseId == PODCAST_LIBRARY_BROWSE_ID) {
                        YouTube.library(browseId).map { page ->
                            BrowsePage(
                                title = page.title,
                                items = page.items,
                                continuation = page.continuation,
                            )
                        }
                    } else {
                        YouTube.browse(browseId, params = null).map { page ->
                            BrowsePage(
                                title = page.title,
                                items = page.items.flatMap { section -> section.items },
                                continuation = null,
                            )
                        }
                    }
                result.rethrowCancellation()
                result
            }

        suspend fun loadContinuation(continuation: String): Result<BrowsePage> =
            withContext(Dispatchers.IO) {
                val result =
                    YouTube.libraryContinuation(continuation).map { page ->
                        BrowsePage(
                            title = null,
                            items = page.items,
                            continuation = page.continuation,
                        )
                    }
                result.rethrowCancellation()
                result
            }
    }

private fun Result<*>.rethrowCancellation() {
    val failure = exceptionOrNull()
    if (failure is CancellationException) throw failure
}
