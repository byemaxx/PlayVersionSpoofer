package com.mymod.playspoofer.xposed

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.Keep

@Keep
fun isModuleActivated(): Boolean = false

val statusIsModuleActivated: Boolean
    @Keep get() = isModuleActivated()


val Context.statusIsPreferencesReady: Boolean
    @SuppressLint("WorldReadableFiles")get() {
        return try {
            @Suppress("DEPRECATION") getSharedPreferences(
                "testPreferences",
                Context.MODE_WORLD_READABLE
            )
            true
        } catch (t: Throwable) {
            android.util.Log.e(Log.TAG, "failed to confirm SharedPreferences' state.", t)
            false
        }
    }
