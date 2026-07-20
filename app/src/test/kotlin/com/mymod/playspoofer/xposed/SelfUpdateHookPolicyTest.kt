package com.mymod.playspoofer.xposed

import com.mymod.playspoofer.preferences.PreferenceContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfUpdateHookPolicyTest {
    @Test
    fun legacyPackageInfoFallbackIsDisabledByDefault() {
        assertFalse(PreferenceContract.DEFAULT_LEGACY_PACKAGE_INFO_FALLBACK)
    }

    @Test
    fun dynamicHookRequiresExactlyOneCandidate() {
        assertFalse(SelfUpdateHookPolicy.hasUniqueCandidate(0))
        assertTrue(SelfUpdateHookPolicy.hasUniqueCandidate(1))
        assertFalse(SelfUpdateHookPolicy.hasUniqueCandidate(2))
    }

    @Test
    fun dynamicHookUsesSharedSchedulerFeaturesFromBothAnalyzedVersions() {
        assertEquals(
            listOf(
                "Skipping DFE self-update check as there is an update already queued.",
                "Bulk scheduling self-update with policies",
            ),
            SelfUpdateHookPolicy.REQUIRED_STRING_FRAGMENTS,
        )
        assertFalse(SelfUpdateHookPolicy.NO_UPDATE_RESULT)
    }

    @Test
    fun dynamicHookCacheOnlyMatchesTheSamePlayStoreVersion() {
        assertTrue(
            DynamicHookCachePolicy.canUse(
                cachedSchemaVersion = DynamicHookCachePolicy.SCHEMA_VERSION,
                cachedPlayStoreVersionCode = 84102830L,
                installedPlayStoreVersionCode = 84102830L,
                descriptor = "Ladfx;->g(Lader;Lkdp;Lkcc;Ljava/lang/Runnable;)Z",
            )
        )
        assertFalse(
            DynamicHookCachePolicy.canUse(
                cachedSchemaVersion = DynamicHookCachePolicy.SCHEMA_VERSION,
                cachedPlayStoreVersionCode = 84102830L,
                installedPlayStoreVersionCode = 84102831L,
                descriptor = "Ladfx;->g(Lader;Lkdp;Lkcc;Ljava/lang/Runnable;)Z",
            )
        )
    }

    @Test
    fun dynamicHookCacheRejectsOldSchemasAndBlankDescriptors() {
        assertFalse(
            DynamicHookCachePolicy.canUse(
                cachedSchemaVersion = DynamicHookCachePolicy.SCHEMA_VERSION - 1,
                cachedPlayStoreVersionCode = 84102830L,
                installedPlayStoreVersionCode = 84102830L,
                descriptor = "Ladfx;->g(Lader;Lkdp;Lkcc;Ljava/lang/Runnable;)Z",
            )
        )
        assertFalse(
            DynamicHookCachePolicy.canUse(
                cachedSchemaVersion = DynamicHookCachePolicy.SCHEMA_VERSION,
                cachedPlayStoreVersionCode = 84102830L,
                installedPlayStoreVersionCode = 84102830L,
                descriptor = " ",
            )
        )
    }
}
