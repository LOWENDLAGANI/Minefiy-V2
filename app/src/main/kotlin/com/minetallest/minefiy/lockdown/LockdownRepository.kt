/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.lockdown

import com.minetallest.minefiy.BuildConfig
import com.minetallest.minefiy.innertube.NetworkGatekeeper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides, for this device, whether the app is allowed to be used.
 *
 * Two layers, and they are deliberately different:
 *
 * 1. `NetworkGatekeeper` — already in the interception chain, so a locked device
 *    cannot fetch a stream, a search or an image even if a screen slipped
 *    through. It starts blocked and is opened by a successful check.
 * 2. The policy in `state` — what the full-screen lock renders.
 *
 * **A failed check never locks anyone out.** The app is offline-first: a train
 * tunnel, a flight or a Firestore outage must not turn a music player into a
 * brick. So a read failure keeps the last policy this process already saw, and
 * with no policy at all the app runs. Only a policy that was actually *read*
 * can close the app. The cost is that a lockdown reaches a device the next time
 * it checks in — which is the right trade for a personal build.
 */
@Singleton
class LockdownRepository
    @Inject
    constructor() {
        private val refreshMutex = Mutex()

        private val _policy = MutableStateFlow<LockdownPolicy?>(null)

        /** Non-null only while the app must be locked, so the gate is a null check. */
        val policy = _policy.asStateFlow()

        @Volatile
        private var lastKnown: LockdownPolicy? = null

        /** True when this build has a Firebase project and key, so the lock can be reached. */
        val configured: Boolean
            get() = FirebaseLockdown.configured

        suspend fun refresh() {
            if (!configured) {
                release()
                return
            }

            refreshMutex.withLock {
                FirebaseLockdown.read()
                    .onSuccess { fresh -> apply(fresh) }
                    .onFailure { error ->
                        // Keep whatever the last successful read said. If there has
                        // never been one, the app stays usable.
                        Timber.w(error, "Lockdown policy could not be read; keeping the last known one")
                        lastKnown?.let { apply(it) }
                    }
            }
        }

        /** Applies a policy the panel just wrote, so the lock answers immediately. */
        fun apply(policy: LockdownPolicy) {
            lastKnown = policy
            val blocked = policy.blocks(BuildConfig.VERSION_CODE)

            NetworkGatekeeper.setConnectionBlocked(blocked)
            _policy.value = if (blocked) policy else null
        }

        private fun release() {
            NetworkGatekeeper.setConnectionBlocked(false)
            _policy.value = null
        }
    }
