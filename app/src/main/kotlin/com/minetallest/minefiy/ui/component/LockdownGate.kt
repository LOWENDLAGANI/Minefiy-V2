/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.minetallest.minefiy.R
import com.minetallest.minefiy.lockdown.LockdownPolicy

/**
 * The lock itself.
 *
 * A dialog rather than a branch inside the navigation graph, for one reason: it
 * covers whatever the app was showing — player, settings, a dialog that was
 * already open — without any screen having to know it exists. Back and an
 * outside tap are both refused, so there is no gesture that dismisses it.
 *
 * The retry button is the only way out, and it leaves the decision where it
 * belongs: the owner clears the policy, the device re-reads it, and the app
 * comes back on its own.
 */
@Composable
fun LockdownGate(
    policy: LockdownPolicy?,
    checking: Boolean,
    onRetry: () -> Unit,
) {
    if (policy == null) return

    Dialog(
        onDismissRequest = {},
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.lockdown_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = policy.message.trim().ifBlank { stringResource(R.string.lockdown_default_message) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .padding(top = 12.dp)
                            .widthIn(max = 360.dp),
                )

                if (policy.minVersionCode > 0) {
                    Text(
                        text = stringResource(R.string.lockdown_update_required),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                Button(
                    onClick = onRetry,
                    enabled = !checking,
                    modifier =
                        Modifier
                            .padding(top = 24.dp)
                            .fillMaxWidth()
                            .widthIn(max = 360.dp),
                ) {
                    Text(
                        text =
                            stringResource(
                                if (checking) R.string.lockdown_checking else R.string.lockdown_retry,
                            ),
                    )
                }
            }
        }
    }
}
