package com.mymod.playspoofer.xposed

import de.robv.android.xposed.XposedBridge

object Log {
    const val TAG = "PlaySpoofer"

    fun d(message: String, throwable: Throwable? = null) {
        log("DEBUG", message, throwable)
    }

    fun i(message: String, throwable: Throwable? = null) {
        log("INFO", message, throwable)
    }

    fun w(message: String, throwable: Throwable? = null) {
        log("WARN", message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        log("ERROR", message, throwable)
    }

    private fun log(level: String, message: String, throwable: Throwable? = null) {
        XposedBridge.log("[$level] $TAG: $message")
        if (throwable != null) {
            XposedBridge.log(throwable)
        }
    }
}
