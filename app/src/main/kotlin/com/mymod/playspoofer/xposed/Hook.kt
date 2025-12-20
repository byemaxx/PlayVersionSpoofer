package com.mymod.playspoofer.xposed

import android.content.pm.PackageInfo
import androidx.annotation.Keep
import com.mymod.playspoofer.BuildConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

@Keep
class Hook : IXposedHookLoadPackage {
    companion object {
        private const val PLAY_STORE_PKG = "com.android.vending"
        private const val MAX_VERSION_CODE = 99999999L
        private const val MAX_VERSION_NAME = "999.999.999"

        /** 标记是否已经在第一次 Hook 成功时打印过日志 */
        @Volatile
        private var hasHookedPlayStore = false

        @Volatile
        private var hasLoggedFlagsHook = false

        @Volatile
        private var hasLoggedNullResult = false

        private const val VERSIONED_PACKAGE_CLASS = "android.content.pm.VersionedPackage"
        private const val PACKAGE_INFO_FLAGS_CLASS = "android.content.pm.PackageManager\$PackageInfoFlags"
        private const val APP_PACKAGE_MANAGER_CLASS = "android.app.ApplicationPackageManager"
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // 1. 针对自身模块的激活状态 Hook
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            try {
                XposedHelpers.findAndHookMethod(
                    "com.mymod.playspoofer.xposed.ModuleStatusKt",
                    lpparam.classLoader,
                    "isModuleActivated",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = true
                            Log.i("Module activation status forced to true")
                        }
                    }
                )
                Log.i("PlaySpoofer 模块已成功加载")
            } catch (e: Throwable) {
                Log.e("Failed to hook module status: ${e.message}", e)
            }
            return
        }

        // 2. 只对 Google Play Store 生效
        if (lpparam.packageName != PLAY_STORE_PKG) return

        // 只在第一次 Hook Play Store 时输出“开始 Hook [进程名]”日志
        if (!hasHookedPlayStore) {
            Log.i("开始 Hook 进程：${lpparam.packageName}")
        }

        if (!ensureApplicationPackageManagerAvailable()) return

        // Hook getPackageInfo(String, int)
        hookGetPackageInfoSafely(
            lpparam,
            arrayOf<Class<*>>(String::class.java, Int::class.javaPrimitiveType!!),
            "getPackageInfo(String, int)"
        )
        // Hook getPackageInfo(String, PackageInfoFlags)
        hookPackageInfoFlagsOverload(
            lpparam,
            String::class.java,
            "getPackageInfo(String, PackageInfoFlags)"
        )
        // Hook getPackageInfo(VersionedPackage, int)
        hookVersionedPackageOverload(
            lpparam,
            Int::class.javaPrimitiveType!!,
            "getPackageInfo(VersionedPackage, int)"
        )
        // Hook getPackageInfo(VersionedPackage, PackageInfoFlags)
        hookVersionedPackageFlagsOverload(lpparam)
    }

    /**
     * 提炼出的公共方法，支持两种签名：
     *   - getPackageInfo(String, int)
     *   - getPackageInfo(VersionedPackage, int)
     */
    private fun hookGetPackageInfo(
        lpparam: LoadPackageParam,
        paramTypes: Array<Class<*>>
    ) {
        val methodHook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                // 获取第一个参数：可能是 String，也可能是 VersionedPackage
                val pkgArg = param.args[0]
                val pkgName = when (pkgArg) {
                    is String -> pkgArg
                    else -> {
                        if (pkgArg.javaClass.name != VERSIONED_PACKAGE_CLASS) return
                        XposedHelpers.callMethod(pkgArg, "getPackageName") as? String ?: return
                    }
                }
                if (pkgName != PLAY_STORE_PKG) return

                // 拿到原始 PackageInfo 对象
                val pkgInfo = param.result as? PackageInfo
                if (pkgInfo == null) {
                    if (!hasLoggedNullResult) {
                        Log.i("getPackageInfo returned null or unexpected result for $pkgName")
                        hasLoggedNullResult = true
                    }
                    return
                }

                if (!hasHookedPlayStore) {
                    // 第一次进入这里：打印原始版本→伪装版本日志，并设置标志
                    logVersion("原始版本", pkgInfo)
                    modifyPackageInfo(pkgInfo)
                    logVersion("已伪装版本", pkgInfo)
                    hasHookedPlayStore = true
                } else {
                    // 后续所有调用：只做版本号修改，不再打印日志
                    modifyPackageInfo(pkgInfo)
                }
                // 必须重新赋值回去
                param.result = pkgInfo
            }
        }

        XposedHelpers.findAndHookMethod(
            APP_PACKAGE_MANAGER_CLASS,
            lpparam.classLoader,
            "getPackageInfo",
            *paramTypes,
            methodHook
        )
    }

    /**
     * 打印版本号信息
     */
    private fun logVersion(tagPrefix: String, packageInfo: PackageInfo) {
        val versionInfo = buildString {
            append("longVersionCode=").append(packageInfo.longVersionCode)
            append(", versionName=").append(packageInfo.versionName)
        }
        Log.i("$tagPrefix -> $versionInfo")
    }

    /**
     * 强制修改 PackageInfo 中的版本号字段为最大值
     */
    private fun modifyPackageInfo(packageInfo: PackageInfo) {
        runCatching { packageInfo.longVersionCode = MAX_VERSION_CODE }
            .onFailure {
                runCatching {
                    XposedHelpers.setLongField(packageInfo, "longVersionCode", MAX_VERSION_CODE)
                }
            }

        runCatching { packageInfo.versionName = MAX_VERSION_NAME }
            .onFailure {
                runCatching {
                    XposedHelpers.setObjectField(packageInfo, "versionName", MAX_VERSION_NAME)
                }
            }
    }

    private fun ensureApplicationPackageManagerAvailable(): Boolean {
        return runCatching {
            Class.forName(APP_PACKAGE_MANAGER_CLASS)
        }.onFailure { error ->
            Log.i("ApplicationPackageManager not available: ${error.javaClass.simpleName}: ${error.message}")
        }.isSuccess
    }

    private fun hookGetPackageInfoSafely(
        lpparam: LoadPackageParam,
        paramTypes: Array<Class<*>>,
        signatureLabel: String
    ) {
        runCatching {
            hookGetPackageInfo(lpparam, paramTypes)
        }.onFailure { error ->
            Log.i("$signatureLabel hook not available: ${error.javaClass.simpleName}: ${error.message}")
        }
    }

    private fun hookPackageInfoFlagsOverload(
        lpparam: LoadPackageParam,
        packageNameClass: Class<*>,
        signatureLabel: String
    ) {
        val flagsClass = resolveClass(PACKAGE_INFO_FLAGS_CLASS)
        if (flagsClass == null) {
            Log.i("$signatureLabel hook not available: PackageInfoFlags class missing")
            return
        }
        hookGetPackageInfoSafely(
            lpparam,
            arrayOf(packageNameClass, flagsClass),
            signatureLabel
        )
        if (!hasLoggedFlagsHook) {
            Log.i("PackageInfoFlags overload detected, enabling flags-based hook")
            hasLoggedFlagsHook = true
        }
    }

    private fun hookVersionedPackageOverload(
        lpparam: LoadPackageParam,
        flagType: Class<*>,
        signatureLabel: String
    ) {
        val versionedClass = resolveClass(VERSIONED_PACKAGE_CLASS)
        if (versionedClass == null) {
            Log.i("VersionedPackage 类不存在，跳过 Hook")
            return
        }
        hookGetPackageInfoSafely(
            lpparam,
            arrayOf(versionedClass, flagType),
            signatureLabel
        )
    }

    private fun hookVersionedPackageFlagsOverload(lpparam: LoadPackageParam) {
        val versionedClass = resolveClass(VERSIONED_PACKAGE_CLASS)
        val flagsClass = resolveClass(PACKAGE_INFO_FLAGS_CLASS)
        if (versionedClass == null || flagsClass == null) {
            Log.i("getPackageInfo(VersionedPackage, PackageInfoFlags) hook not available: missing class")
            return
        }
        hookGetPackageInfoSafely(
            lpparam,
            arrayOf(versionedClass, flagsClass),
            "getPackageInfo(VersionedPackage, PackageInfoFlags)"
        )
        if (!hasLoggedFlagsHook) {
            Log.i("PackageInfoFlags overload detected, enabling flags-based hook")
            hasLoggedFlagsHook = true
        }
    }

    private fun resolveClass(className: String): Class<*>? {
        return runCatching { Class.forName(className) }.getOrNull()
    }
}
