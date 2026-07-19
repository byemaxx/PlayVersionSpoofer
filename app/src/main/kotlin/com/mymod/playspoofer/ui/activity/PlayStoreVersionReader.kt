package com.mymod.playspoofer.ui.activity

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.mymod.playspoofer.xposed.SpoofPolicy

internal sealed interface PlayStoreVersionState {
    data class Installed(
        val versionName: String?,
        val versionCode: Long,
    ) : PlayStoreVersionState

    data object NotInstalled : PlayStoreVersionState

    data object Unavailable : PlayStoreVersionState
}

internal object PlayStoreVersionReader {
    fun read(context: Context): PlayStoreVersionState {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    SpoofPolicy.TARGET_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(SpoofPolicy.TARGET_PACKAGE, 0)
            }

            PlayStoreVersionState.Installed(
                versionName = packageInfo.versionName?.takeIf(String::isNotBlank),
                versionCode = packageInfo.longVersionCode,
            )
        } catch (_: PackageManager.NameNotFoundException) {
            PlayStoreVersionState.NotInstalled
        } catch (_: RuntimeException) {
            PlayStoreVersionState.Unavailable
        }
    }
}
