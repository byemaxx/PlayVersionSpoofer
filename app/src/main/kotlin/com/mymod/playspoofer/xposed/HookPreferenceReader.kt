package com.mymod.playspoofer.xposed

import android.content.Context
import android.net.Uri
import com.mymod.playspoofer.BuildConfig
import com.mymod.playspoofer.preferences.PreferenceContract
import de.robv.android.xposed.XSharedPreferences

internal object HookPreferenceReader {
    fun isLegacyPackageInfoFallbackEnabled(context: Context): Boolean {
        readFromPreferenceProvider(context)?.let { return it }
        return readFromXSharedPreferences()
    }

    private fun readFromPreferenceProvider(context: Context): Boolean? {
        return try {
            val uri = Uri.Builder()
                .scheme("content")
                .authority(PreferenceContract.PROVIDER_AUTHORITY)
                .build()
            val result = context.contentResolver.call(
                uri,
                PreferenceContract.METHOD_GET_LEGACY_PACKAGE_INFO_FALLBACK,
                null,
                null,
            ) ?: return null
            if (!result.containsKey(PreferenceContract.RESULT_ENABLED)) return null
            result.getBoolean(PreferenceContract.RESULT_ENABLED)
        } catch (e: Throwable) {
            Log.e("Failed to read legacy fallback preference through provider", e)
            null
        }
    }

    private fun readFromXSharedPreferences(): Boolean {
        return try {
            XSharedPreferences(BuildConfig.APPLICATION_ID, PreferenceContract.FILE_NAME).run {
                getBoolean(
                    PreferenceContract.LEGACY_PACKAGE_INFO_FALLBACK,
                    PreferenceContract.DEFAULT_LEGACY_PACKAGE_INFO_FALLBACK,
                )
            }
        } catch (e: Throwable) {
            Log.e("Failed to read legacy fallback preference through XSharedPreferences", e)
            PreferenceContract.DEFAULT_LEGACY_PACKAGE_INFO_FALLBACK
        }
    }
}
