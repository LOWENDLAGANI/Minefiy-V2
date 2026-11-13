/*
 * Minefiy (2026) — © minetallest
 * Based on ArchiveTune — © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.minetallest.minefiy.lockdown

import org.json.JSONObject

/**
 * The one document the admin panel writes and every installed app reads.
 *
 * It lives at `config/lockdown` in Firestore. Two controls share it because they
 * answer the same question — "may this build be used right now" — and splitting
 * them into two documents would let a device read one and miss the other:
 *
 * - `locked` blocks every install, whatever its version. The kill switch.
 * - `minVersionCode` blocks only builds older than the one named, so a device
 *   that has updated walks through on its own.
 *
 * `updatedAt` is epoch milliseconds. It is written by the panel and shown there,
 * so an owner can tell a fresh policy from one they forgot about months ago.
 */
data class LockdownPolicy(
    val locked: Boolean = false,
    val message: String = "",
    val minVersionCode: Int = 0,
    val updatedAt: Long = 0L,
) {
    /** True when this build must not be used. */
    fun blocks(versionCode: Int): Boolean =
        locked || (minVersionCode > 0 && versionCode < minVersionCode)

    companion object {
        val Default = LockdownPolicy()

        /**
         * Reads Firestore's typed-value envelope.
         *
         * The REST API does not return plain JSON: every field is a
         * `{"booleanValue": …}` object. Parsing it here, once, keeps the rest of
         * the app working in ordinary Kotlin values.
         */
        fun fromFirestore(body: String): LockdownPolicy? {
            val fields = runCatching { JSONObject(body).optJSONObject("fields") }.getOrNull() ?: return null

            return LockdownPolicy(
                locked = fields.optJSONObject("locked")?.optBoolean("booleanValue") ?: false,
                message = fields.optJSONObject("message")?.optString("stringValue").orEmpty(),
                minVersionCode =
                    fields
                        .optJSONObject("minVersionCode")
                        ?.optString("integerValue")
                        ?.toIntOrNull() ?: 0,
                updatedAt =
                    fields
                        .optJSONObject("updatedAt")
                        ?.optString("integerValue")
                        ?.toLongOrNull() ?: 0L,
            )
        }

        /** The same envelope, on the way out. */
        fun toFirestoreBody(policy: LockdownPolicy): String =
            JSONObject()
                .put(
                    "fields",
                    JSONObject()
                        .put("locked", JSONObject().put("booleanValue", policy.locked))
                        .put("message", JSONObject().put("stringValue", policy.message))
                        .put(
                            "minVersionCode",
                            JSONObject().put("integerValue", policy.minVersionCode.toString()),
                        )
                        .put("updatedAt", JSONObject().put("integerValue", policy.updatedAt.toString())),
                ).toString()
    }
}
