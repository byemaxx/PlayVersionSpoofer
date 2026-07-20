package com.mymod.playspoofer.xposed

import android.content.Context

internal object DynamicHookCachePolicy {
    const val SCHEMA_VERSION = 1

    fun canUse(
        cachedSchemaVersion: Int,
        cachedPlayStoreVersionCode: Long,
        installedPlayStoreVersionCode: Long,
        descriptor: String?,
    ): Boolean {
        return cachedSchemaVersion == SCHEMA_VERSION &&
            cachedPlayStoreVersionCode == installedPlayStoreVersionCode &&
            !descriptor.isNullOrBlank()
    }
}

/**
 * The cache lives in Play Store's private data because this code runs under the Play Store UID.
 * A versionCode change always causes a cache miss, so DexKit is loaded only when needed.
 */
internal class DynamicHookCache(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun readDescriptor(installedVersionCode: Long): String? {
        val schemaVersion = preferences.getInt(KEY_SCHEMA_VERSION, 0)
        val cachedVersionCode = preferences.getLong(KEY_PLAY_STORE_VERSION_CODE, -1L)
        val descriptor = preferences.getString(KEY_METHOD_DESCRIPTOR, null)
        return descriptor.takeIf {
            DynamicHookCachePolicy.canUse(
                cachedSchemaVersion = schemaVersion,
                cachedPlayStoreVersionCode = cachedVersionCode,
                installedPlayStoreVersionCode = installedVersionCode,
                descriptor = it,
            )
        }
    }

    fun writeDescriptor(installedVersionCode: Long, descriptor: String) {
        val saved = preferences.edit()
            .putInt(KEY_SCHEMA_VERSION, DynamicHookCachePolicy.SCHEMA_VERSION)
            .putLong(KEY_PLAY_STORE_VERSION_CODE, installedVersionCode)
            .putString(KEY_METHOD_DESCRIPTOR, descriptor)
            .commit()
        if (!saved) {
            Log.e("Failed to persist the dynamic hook cache; the next launch will search again")
        }
    }

    fun clear() {
        preferences.edit().clear().commit()
    }

    private companion object {
        const val FILE_NAME = "playspoofer_dynamic_hook_cache"
        const val KEY_SCHEMA_VERSION = "schema_version"
        const val KEY_PLAY_STORE_VERSION_CODE = "play_store_version_code"
        const val KEY_METHOD_DESCRIPTOR = "method_descriptor"
    }
}
