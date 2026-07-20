package com.mymod.playspoofer.xposed

internal object AutoUpdateHookPolicy {
    const val ANCHOR_STRING = "UChk: document %s is not qualified for auto update v2"

    fun withoutPlayStore(packages: List<*>): List<Any?> {
        return packages.filterNot { it == SpoofPolicy.TARGET_PACKAGE }
    }

    fun containsPlayStore(packages: List<*>): Boolean {
        return packages.any { it == SpoofPolicy.TARGET_PACKAGE }
    }
}
