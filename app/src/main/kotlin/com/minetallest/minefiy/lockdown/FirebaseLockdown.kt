/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.lockdown

import com.minetallest.minefiy.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.IOException

/**
 * Firestore over HTTPS, deliberately without the Firebase SDK.
 *
 * The project already carries an OkHttp/Ktor stack and builds its keys from
 * `local.properties`, so two REST calls and an Identity Toolkit sign-in do the
 * whole job: no `google-services.json`, no plugin, no Play-services Firebase
 * inside the APK, and nothing to break when the Gradle plugin moves.
 *
 * The endpoints used, all documented and stable:
 *
 * - read  `GET  firestore.googleapis.com/v1/projects/<id>/databases/(default)/documents/config/lockdown`
 * - write `PATCH` the same address, with the admin's Firebase ID token
 * - sign-in `POST identitytoolkit.googleapis.com/v1/accounts:signInWithPassword`
 *
 * Reads use the API key and are allowed by the security rules. Writes carry an
 * ID token, so the rules — not this code — decide who may change the policy.
 */
object FirebaseLockdown {
    /** True when this build was given a project and a key. */
    val configured: Boolean
        get() =
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
                BuildConfig.FIREBASE_API_KEY.isNotBlank()

    private val client =
        HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            }
        }

    private fun documentUrl(): String =
        "https://firestore.googleapis.com/v1/projects/" +
            "${BuildConfig.FIREBASE_PROJECT_ID}" +
            "/databases/(default)/documents/$DOCUMENT_PATH" +
            "?key=${BuildConfig.FIREBASE_API_KEY}"

    /**
     * The current policy.
     *
     * A missing document is not an error: it means nobody has ever written a
     * policy, so the app is allowed. A network failure is an error, and the
     * caller decides what to do with it — it must never be read as "blocked".
     */
    suspend fun read(): Result<LockdownPolicy> {
        return try {
            val response = client.get(documentUrl())

            when {
                response.status == HttpStatusCode.OK -> {
                    val policy = LockdownPolicy.fromFirestore(response.bodyAsText())
                    if (policy == null) {
                        Result.failure(IOException("The policy document could not be read."))
                    } else {
                        Result.success(policy)
                    }
                }

                response.status == HttpStatusCode.NotFound -> Result.success(LockdownPolicy.Default)

                else ->
                    Result.failure(
                        IOException("Firestore answered ${response.status.value}."),
                    )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /** Signs the admin in with email and password, returning a short-lived ID token. */
    suspend fun signIn(
        email: String,
        password: String,
    ): Result<String> {
        return try {
            val response =
                client.post(signInUrl()) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        JSONObject()
                            .put("email", email.trim())
                            .put("password", password)
                            .put("returnSecureToken", true)
                            .toString(),
                    )
                }

            val body = response.bodyAsText()
            val token =
                if (response.status == HttpStatusCode.OK) {
                    runCatching { JSONObject(body).optString("idToken") }.getOrDefault("")
                } else {
                    ""
                }

            if (token.isBlank()) {
                Result.failure(IOException(signInMessage(body)))
            } else {
                Result.success(token)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /** Writes the policy. The ID token is what the security rules check. */
    suspend fun write(
        idToken: String,
        policy: LockdownPolicy,
    ): Result<Unit> {
        return try {
            val response =
                client.request(documentUrl()) {
                    method = HttpMethod.Patch
                    header(HttpHeaders.Authorization, "Bearer $idToken")
                    contentType(ContentType.Application.Json)
                    setBody(LockdownPolicy.toFirestoreBody(policy))
                }

            if (response.status == HttpStatusCode.OK) {
                Result.success(Unit)
            } else {
                Result.failure(
                    IOException(
                        if (response.status == HttpStatusCode.Forbidden ||
                            response.status == HttpStatusCode.Unauthorized
                        ) {
                            "Firestore refused the write. Sign in with an account the rules allow."
                        } else {
                            "Firestore answered ${response.status.value}."
                        },
                    ),
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun signInUrl(): String =
        "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword" +
            "?key=${BuildConfig.FIREBASE_API_KEY}"

    /**
     * Identity Toolkit reports failures as machine codes. Nobody should have to
     * read `INVALID_LOGIN_CREDENTIALS` to learn they mistyped a password.
     */
    private fun signInMessage(body: String): String {
        val code =
            runCatching {
                JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
            }.getOrDefault("")

        return when {
            code.startsWith("EMAIL_NOT_FOUND") ||
                code.startsWith("INVALID_PASSWORD") ||
                code.startsWith("INVALID_LOGIN_CREDENTIALS") -> "That email and password do not match an admin account."
            code.startsWith("TOO_MANY_ATTEMPTS_TRY_LATER") -> "Too many attempts. Wait a minute and try again."
            code.startsWith("USER_DISABLED") -> "That admin account is disabled."
            code.startsWith("OPERATION_NOT_ALLOWED") ->
                "Email and password sign-in is switched off in the Firebase console."
            else -> "Sign-in failed. Check the email, the password and the connection."
        }
    }

    private const val DOCUMENT_PATH = "config/lockdown"
    private const val CONNECT_TIMEOUT_MILLIS = 10_000L
    private const val REQUEST_TIMEOUT_MILLIS = 15_000L
}
