package com.mymod.playspoofer.xposed

import android.content.Context

internal object DynamicHookCachePolicy {
    const val SCHEMA_VERSION = 2

    fun canUse(
        cachedSchemaVersion: Int,
        cachedPlayStoreVersionCode: Long,
        installedPlayStoreVersionCode: Long,
        schedulerDescriptor: String?,
        autoUpdateScanComplete: Boolean,
    ): Boolean {
        return cachedSchemaVersion == SCHEMA_VERSION &&
            cachedPlayStoreVersionCode == installedPlayStoreVersionCode &&
            !schedulerDescriptor.isNullOrBlank() &&
            autoUpdateScanComplete
    }
}

internal data class DynamicHookTargets(
    val schedulerDescriptor: String,
    val autoUpdateDescriptor: String?,
)

/**
 * The cache lives in Play Store's private data because this code runs under the Play Store UID.
 * A versionCode change always causes a cache miss, so DexKit is loaded only when needed.
 */
internal class DynamicHookCache(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun readTargets(installedVersionCode: Long): DynamicHookTargets? {
        val schemaVersion = preferences.getInt(KEY_SCHEMA_VERSION, 0)
        val cachedVersionCode = preferences.getLong(KEY_PLAY_STORE_VERSION_CODE, -1L)
        val schedulerDescriptor = preferences.getString(KEY_SCHEDULER_METHOD_DESCRIPTOR, null)
        val autoUpdateScanComplete = preferences.getBoolean(KEY_AUTO_UPDATE_SCAN_COMPLETE, false)
        if (!DynamicHookCachePolicy.canUse(
                cachedSchemaVersion = schemaVersion,
                cachedPlayStoreVersionCode = cachedVersionCode,
                installedPlayStoreVersionCode = installedVersionCode,
                schedulerDescriptor = schedulerDescriptor,
                autoUpdateScanComplete = autoUpdateScanComplete,
            )
        ) {
            return null
        }

        return DynamicHookTargets(
            schedulerDescriptor = checkNotNull(schedulerDescriptor),
            autoUpdateDescriptor = preferences
                .getString(KEY_AUTO_UPDATE_METHOD_DESCRIPTOR, null)
                ?.takeIf(String::isNotBlank),
        )
    }

    fun writeTargets(installedVersionCode: Long, targets: DynamicHookTargets) {
        val editor = preferences.edit()
            .putInt(KEY_SCHEMA_VERSION, DynamicHookCachePolicy.SCHEMA_VERSION)
            .putLong(KEY_PLAY_STORE_VERSION_CODE, installedVersionCode)
            .putString(KEY_SCHEDULER_METHOD_DESCRIPTOR, targets.schedulerDescriptor)
            .putBoolean(KEY_AUTO_UPDATE_SCAN_COMPLETE, true)

        if (targets.autoUpdateDescriptor == null) {
            editor.remove(KEY_AUTO_UPDATE_METHOD_DESCRIPTOR)
        } else {
            editor.putString(KEY_AUTO_UPDATE_METHOD_DESCRIPTOR, targets.autoUpdateDescriptor)
        }

        val saved = editor.commit()
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
        const val KEY_SCHEDULER_METHOD_DESCRIPTOR = "scheduler_method_descriptor"
        const val KEY_AUTO_UPDATE_METHOD_DESCRIPTOR = "auto_update_method_descriptor"
        const val KEY_AUTO_UPDATE_SCAN_COMPLETE = "auto_update_scan_complete"
    }
}
