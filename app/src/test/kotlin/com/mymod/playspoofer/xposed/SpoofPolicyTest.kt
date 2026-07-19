package com.mymod.playspoofer.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpoofPolicyTest {
    @Test
    fun targetsOnlyGooglePlayStore() {
        assertTrue(SpoofPolicy.shouldSpoof("com.android.vending"))
        assertFalse(SpoofPolicy.shouldSpoof("com.google.android.gms"))
        assertFalse(SpoofPolicy.shouldSpoof(null))
    }

    @Test
    fun exposesExpectedSpoofedVersion() {
        assertEquals(99999999L, SpoofPolicy.VERSION_CODE)
        assertEquals("999.999.999", SpoofPolicy.VERSION_NAME)
    }
}
