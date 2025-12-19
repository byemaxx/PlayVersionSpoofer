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

        // Hook getPackageInfo(String, int)
        hookGetPackageInfo(
            lpparam,
            "getPackageInfo",
            arrayOf<Class<*>>(String::class.java, Int::class.javaPrimitiveType!!)
        )
        // Hook getPackageInfo(VersionedPackage, int)
        try {
            val versionedClass = Class.forName("android.content.pm.VersionedPackage")
            hookGetPackageInfo(
                lpparam,
                "getPackageInfo",
                arrayOf<Class<*>>(versionedClass, Int::class.javaPrimitiveType!!)
            )
        } catch (e: ClassNotFoundException) {
            Log.i("VersionedPackage 类不存在，跳过第二个 Hook")
        }

        val packageInfoFlagsClass = try {
            Class.forName("android.content.pm.PackageManager\$PackageInfoFlags")
        } catch (e: ClassNotFoundException) {
            Log.i("PackageInfoFlags 类不存在，跳过对应 Hook")
            null
        }

        if (packageInfoFlagsClass != null) {
            try {
                hookGetPackageInfo(
                    lpparam,
                    "getPackageInfo",
                    arrayOf<Class<*>>(String::class.java, packageInfoFlagsClass)
                )
            } catch (e: Throwable) {
                Log.i("getPackageInfo(String, PackageInfoFlags) 不存在，跳过 Hook")
            }
            try {
                val versionedClass = Class.forName("android.content.pm.VersionedPackage")
                try {
                    hookGetPackageInfo(
                        lpparam,
                        "getPackageInfo",
                        arrayOf<Class<*>>(versionedClass, packageInfoFlagsClass)
                    )
                } catch (e: Throwable) {
                    Log.i("getPackageInfo(VersionedPackage, PackageInfoFlags) 不存在，跳过 Hook")
                }
            } catch (e: ClassNotFoundException) {
                Log.i("VersionedPackage 类不存在，跳过 PackageInfoFlags 重载 Hook")
            }
            try {
                hookGetPackageInfo(
                    lpparam,
                    "getPackageInfoAsUser",
                    arrayOf<Class<*>>(
                        String::class.java,
                        packageInfoFlagsClass,
                        Int::class.javaPrimitiveType!!
                    )
                )
            } catch (e: Throwable) {
                Log.i("getPackageInfoAsUser(String, PackageInfoFlags, int) 不存在，尝试旧签名")
                try {
                    hookGetPackageInfo(
                        lpparam,
                        "getPackageInfoAsUser",
                        arrayOf<Class<*>>(
                            String::class.java,
                            Int::class.javaPrimitiveType!!,
                            Int::class.javaPrimitiveType!!
                        )
                    )
                } catch (inner: Throwable) {
                    Log.i("getPackageInfoAsUser(String, int, int) 不存在，跳过 Hook")
                }
            }
        } else {
            try {
                hookGetPackageInfo(
                    lpparam,
                    "getPackageInfoAsUser",
                    arrayOf<Class<*>>(
                        String::class.java,
                        Int::class.javaPrimitiveType!!,
                        Int::class.javaPrimitiveType!!
                    )
                )
            } catch (e: Throwable) {
                Log.i("getPackageInfoAsUser(String, int, int) 不存在，跳过 Hook")
            }
        }
    }

    /**
     * 提炼出的公共方法，支持两种签名：
     *   - getPackageInfo(String, int)
     *   - getPackageInfo(VersionedPackage, int)
     */
    private fun hookGetPackageInfo(
        lpparam: LoadPackageParam,
        methodName: String,
        paramTypes: Array<Class<*>>
    ) {
        val methodHook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                // 获取第一个参数：可能是 String，也可能是 VersionedPackage
                val pkgArg = param.args[0]
                val pkgName = when (pkgArg) {
                    is String -> pkgArg
                    else -> XposedHelpers.callMethod(pkgArg, "getPackageName") as? String ?: return
                }
                if (pkgName != PLAY_STORE_PKG) return

                // 拿到原始 PackageInfo 对象
                (param.result as? PackageInfo)?.let { pkgInfo ->
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
        }

        XposedHelpers.findAndHookMethod(
            "android.app.ApplicationPackageManager",
            lpparam.classLoader,
            methodName,
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
        packageInfo.apply {
            longVersionCode = MAX_VERSION_CODE
            versionName = MAX_VERSION_NAME
        }
    }
}
