package com.mymod.playspoofer.xposed

import androidx.annotation.Keep

@Keep
fun isModuleActivated(): Boolean = false

val statusIsModuleActivated: Boolean
    @Keep get() = isModuleActivated()
