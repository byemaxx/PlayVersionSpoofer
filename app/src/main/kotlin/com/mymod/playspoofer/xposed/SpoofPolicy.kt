package com.mymod.playspoofer.xposed

internal object SpoofPolicy {
    const val TARGET_PACKAGE = "com.android.vending"
    const val VERSION_CODE = 99999999L
    const val VERSION_NAME = "999.999.999"

    fun shouldSpoof(packageName: String?): Boolean = packageName == TARGET_PACKAGE
}
