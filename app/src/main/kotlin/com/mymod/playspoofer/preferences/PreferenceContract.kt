package com.mymod.playspoofer.preferences

internal object PreferenceContract {
    const val FILE_NAME = "hook_preferences"
    const val LEGACY_PACKAGE_INFO_FALLBACK = "legacy_package_info_fallback"
    const val DEFAULT_LEGACY_PACKAGE_INFO_FALLBACK = false

    const val PROVIDER_AUTHORITY = "com.mymod.playspoofer.preferences"
    const val METHOD_GET_LEGACY_PACKAGE_INFO_FALLBACK =
        "get_legacy_package_info_fallback"
    const val RESULT_ENABLED = "enabled"
}
