package com.mymod.playspoofer.xposed

import com.mymod.playspoofer.BuildConfig
import com.mymod.playspoofer.preferences.PreferenceContract
import de.robv.android.xposed.XSharedPreferences

internal object HookPreferenceReader {
    fun isLegacyPackageInfoFallbackEnabled(): Boolean {
        return try {
            XSharedPreferences(BuildConfig.APPLICATION_ID, PreferenceContract.FILE_NAME).run {
                getBoolean(
                    PreferenceContract.LEGACY_PACKAGE_INFO_FALLBACK,
                    PreferenceContract.DEFAULT_LEGACY_PACKAGE_INFO_FALLBACK,
                )
            }
        } catch (e: Throwable) {
            Log.e("Failed to read legacy fallback preference; keeping it disabled", e)
            PreferenceContract.DEFAULT_LEGACY_PACKAGE_INFO_FALLBACK
        }
    }
}
