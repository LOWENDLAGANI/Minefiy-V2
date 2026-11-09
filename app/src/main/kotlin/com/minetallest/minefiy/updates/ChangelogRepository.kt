/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.updates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import com.minetallest.minefiy.utils.ReleaseInfo
import com.minetallest.minefiy.utils.Updater
import javax.inject.Inject

class ChangelogRepository @Inject constructor() {
    fun observeReleases(): Flow<List<ReleaseInfo>> =
        flow {
            val cachedReleases = Updater.getCachedReleases()
            if (cachedReleases.isNotEmpty()) emit(cachedReleases)
            emit(Updater.getAllReleases(forceRefresh = true).getOrThrow())
        }.flowOn(Dispatchers.IO)
}
