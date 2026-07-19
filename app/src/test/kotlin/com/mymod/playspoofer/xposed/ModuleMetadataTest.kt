package com.mymod.playspoofer.xposed

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleMetadataTest {
    @Test
    fun xposedEntryPointIsPackaged() {
        assertEquals(
            "com.mymod.playspoofer.xposed.Hook",
            sourceFile("assets/xposed_init").readText().trim(),
        )
    }

    @Test
    fun scopeMetadataMatchesCurrentPurpose() {
        val scope = sourceFile("assets/scope.json").readText()

        assertTrue(scope.contains(SpoofPolicy.TARGET_PACKAGE))
        assertTrue(scope.contains("阻止其自动更新"))
        assertFalse(scope.contains("Android 12L"))
    }

    @Test
    fun manifestDeclaresPlayStorePackageVisibility() {
        val manifest = sourceFile("AndroidManifest.xml").readText()

        assertTrue(manifest.contains("<package android:name=\"${SpoofPolicy.TARGET_PACKAGE}\""))
    }

    private fun sourceFile(relativePath: String): File {
        val workingDirectory = File(checkNotNull(System.getProperty("user.dir")))
        return sequenceOf(
            File(workingDirectory, "src/main/$relativePath"),
            File(workingDirectory, "app/src/main/$relativePath"),
        ).first { it.isFile }
    }
}
