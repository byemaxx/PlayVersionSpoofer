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
                schedulerDescriptor = "Ladfx;->g(Lader;Lkdp;Lkcc;Ljava/lang/Runnable;)Z",
                autoUpdateScanComplete = true,
            )
        )
        assertFalse(
            DynamicHookCachePolicy.canUse(
                cachedSchemaVersion = DynamicHookCachePolicy.SCHEMA_VERSION,
                cachedPlayStoreVersionCode = 84102830L,
                installedPlayStoreVersionCode = 84102831L,
                schedulerDescriptor = "Ladfx;->g(Lader;Lkdp;Lkcc;Ljava/lang/Runnable;)Z",
                autoUpdateScanComplete = true,
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
                schedulerDescriptor = "Ladfx;->g(Lader;Lkdp;Lkcc;Ljava/lang/Runnable;)Z",
                autoUpdateScanComplete = true,
            )
        )
        assertFalse(
            DynamicHookCachePolicy.canUse(
                cachedSchemaVersion = DynamicHookCachePolicy.SCHEMA_VERSION,
                cachedPlayStoreVersionCode = 84102830L,
                installedPlayStoreVersionCode = 84102830L,
                schedulerDescriptor = " ",
                autoUpdateScanComplete = true,
            )
        )
        assertFalse(
            DynamicHookCachePolicy.canUse(
                cachedSchemaVersion = DynamicHookCachePolicy.SCHEMA_VERSION,
                cachedPlayStoreVersionCode = 84102830L,
                installedPlayStoreVersionCode = 84102830L,
                schedulerDescriptor = "Ladfx;->g(Lader;Lkdp;Lkcc;Ljava/lang/Runnable;)Z",
                autoUpdateScanComplete = false,
            )
        )
    }

    @Test
    fun autoUpdateFilterOnlyRemovesPlayStore() {
        val packages = listOf(
            "com.example.one",
            SpoofPolicy.TARGET_PACKAGE,
            "com.example.two",
        )

        assertTrue(AutoUpdateHookPolicy.containsPlayStore(packages))
        assertEquals(
            listOf("com.example.one", "com.example.two"),
            AutoUpdateHookPolicy.withoutPlayStore(packages),
        )
        assertFalse(AutoUpdateHookPolicy.containsPlayStore(listOf("com.example.one")))
    }
}
