/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.minetallest.minefiy.R
import com.minetallest.minefiy.lockdown.AdminPanelViewModel
import com.minetallest.minefiy.lockdown.FirebaseLockdown
import com.minetallest.minefiy.lockdown.LockdownPolicy
import com.minetallest.minefiy.ui.component.IconButton
import kotlinx.coroutines.launch

/**
 * The panel, reached by ten taps on the About logo.
 *
 * It is hidden, not secured: the gesture keeps it away from someone poking
 * around the settings, and **the Firestore rules are what actually decide who
 * can write**. A panel that trusted a hidden gesture would be a panel anyone
 * could drive by decompiling the APK, so the sign-in here is a real Firebase
 * account, and the rules only allow the owner's uid.
 *
 * The three controls are the whole product:
 * - lock everything now,
 * - say why,
 * - name the oldest build that may keep working.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    navController: NavController,
    viewModel: AdminPanelViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var idToken by remember { mutableStateOf<String?>(null) }

    var locked by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var minVersion by remember { mutableStateOf("0") }

    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    val configured = FirebaseLockdown.configured

    LaunchedEffect(configured) {
        if (!configured) return@LaunchedEffect
        FirebaseLockdown.read()
            .onSuccess { policy ->
                locked = policy.locked
                message = policy.message
                minVersion = policy.minVersionCode.toString()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_panel)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        androidx.compose.material3.Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back_button_desc),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!configured) {
                Text(
                    text = stringResource(R.string.admin_not_configured),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            if (idToken == null) {
                Text(
                    text = stringResource(R.string.admin_sign_in_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.admin_email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.admin_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            status = null
                            FirebaseLockdown.signIn(email, password)
                                .onSuccess { idToken = it }
                                .onFailure { status = it.message }
                            busy = false
                        }
                    },
                    enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.admin_sign_in))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.admin_signed_in),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    OutlinedButton(
                        onClick = {
                            idToken = null
                            password = ""
                            status = null
                        },
                    ) {
                        Text(stringResource(R.string.admin_sign_out))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.admin_lock_all),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.admin_lock_all_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = locked, onCheckedChange = { locked = it })
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(stringResource(R.string.admin_message)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = minVersion,
                    onValueChange = { input -> minVersion = input.filter { it.isDigit() }.take(9) },
                    label = { Text(stringResource(R.string.admin_min_version)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = {
                        val token = idToken ?: return@Button
                        scope.launch {
                            busy = true
                            status = null

                            val policy =
                                LockdownPolicy(
                                    locked = locked,
                                    message = message,
                                    minVersionCode = minVersion.toIntOrNull() ?: 0,
                                    updatedAt = System.currentTimeMillis(),
                                )

                            FirebaseLockdown.write(token, policy)
                                .onSuccess {
                                    // Apply it here too, so this device answers at once
                                    // instead of waiting for the next read.
                                    viewModel.repository.apply(policy)
                                    status = "Policy saved."
                                }
                                .onFailure { status = it.message }

                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.admin_save))
                }
            }

            if (busy) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 4.dp))
            }

            status?.let { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
