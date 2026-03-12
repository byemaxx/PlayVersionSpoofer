package com.mymod.playspoofer.ui.activity

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object LauncherIconManager {
    private const val LAUNCHER_ALIAS_NAME = "com.mymod.playspoofer.ui.activity.MainActivityLauncher"

    fun isVisible(context: Context): Boolean {
        return isEnabled(
            context = context,
            componentClassName = LAUNCHER_ALIAS_NAME,
            manifestDefaultEnabled = true
        )
    }

    fun setVisible(context: Context, visible: Boolean) {
        setEnabled(
            context = context,
            componentClassName = LAUNCHER_ALIAS_NAME,
            state = if (visible) {
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
        )
    }

    private fun isEnabled(
        context: Context,
        componentClassName: String,
        manifestDefaultEnabled: Boolean,
    ): Boolean {
        val componentName = ComponentName(context, componentClassName)
        return when (context.packageManager.getComponentEnabledSetting(componentName)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
            else -> manifestDefaultEnabled
        }
    }

    private fun setEnabled(
        context: Context,
        componentClassName: String,
        state: Int,
    ) {
        val componentName = ComponentName(context, componentClassName)
        context.packageManager.setComponentEnabledSetting(
            componentName,
            state,
            PackageManager.DONT_KILL_APP
        )
    }
}
