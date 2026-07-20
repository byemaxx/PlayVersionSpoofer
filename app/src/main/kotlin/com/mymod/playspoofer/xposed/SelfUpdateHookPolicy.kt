package com.mymod.playspoofer.xposed

internal object SelfUpdateHookPolicy {
    const val NO_UPDATE_RESULT = false

    val REQUIRED_STRING_FRAGMENTS = listOf(
        "Skipping DFE self-update check as there is an update already queued.",
        "Bulk scheduling self-update with policies",
    )

    fun hasUniqueCandidate(candidateCount: Int): Boolean = candidateCount == 1
}
