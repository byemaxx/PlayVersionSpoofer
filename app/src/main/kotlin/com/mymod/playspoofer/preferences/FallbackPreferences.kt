package com.mymod.playspoofer.preferences

import android.content.Context

internal object FallbackPreferences {
    fun isLegacyPackageInfoFallbackEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PreferenceContract.FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(
                PreferenceContract.LEGACY_PACKAGE_INFO_FALLBACK,
                PreferenceContract.DEFAULT_LEGACY_PACKAGE_INFO_FALLBACK,
            )
    }

    fun setLegacyPackageInfoFallbackEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PreferenceContract.FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PreferenceContract.LEGACY_PACKAGE_INFO_FALLBACK, enabled)
            .apply()
    }
}
