/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import com.minetallest.minefiy.BuildConfig
import com.minetallest.minefiy.constants.AutomaticUpdateCheckKey

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!BuildConfig.UPDATER_AVAILABLE) {
            return Result.success()
        }

        return try {
            val dataStore = applicationContext.dataStore

            val preferences = dataStore.data.first()
            val automaticChecksEnabled = preferences[AutomaticUpdateCheckKey] ?: true
            if (!automaticChecksEnabled) return Result.success()

            val latestVersion = Updater.getLatestVersionName().getOrElse { throw it }

            if (Updater.isUpdateAvailable(latestVersion, BuildConfig.VERSION_NAME)) {
                UpdateNotificationManager.notifyIfNewVersion(
                    applicationContext,
                    latestVersion,
                )
            }

            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            reportException(exception)
            Result.retry()
        }
    }
}
