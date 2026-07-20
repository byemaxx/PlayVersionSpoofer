package com.mymod.playspoofer.xposed

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import androidx.annotation.Keep
import com.mymod.playspoofer.BuildConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.wrap.DexMethod
import java.lang.reflect.Method

@Keep
class Hook : IXposedHookLoadPackage {
    companion object {
        /** Tracks whether the first successful legacy hook has already been logged. */
        @Volatile
        private var hasHookedPlayStore = false

        @Volatile
        private var hasBlockedSelfUpdate = false

        @Volatile
        private var hasInitializedPlayStoreHooks = false
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // 1. Override this module's activation status when Xposed loads it.
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            try {
                XposedHelpers.findAndHookMethod(
                    "com.mymod.playspoofer.xposed.ModuleStatusKt",
                    lpparam.classLoader,
                    "isModuleActivated",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = true
                            if (BuildConfig.DEBUG) {
                                Log.i("Module activation status forced to true")
                            }
                        }
                    }
                )
                Log.i("PlaySpoofer module loaded successfully")
            } catch (e: Throwable) {
                Log.e("Failed to hook module status: ${e.message}", e)
            }
            return
        }

        // 2. Only affect Google Play Store.
        if (!SpoofPolicy.shouldSpoof(lpparam.packageName)) return

        // Application.attach provides a Context for keying the cache by the real versionCode.
        // It still runs before Application.onCreate and any self-update work.
        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "attach",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.args[0] as? Context ?: return
                        initializePlayStoreHooks(lpparam, context)
                    }
                }
            )
        } catch (e: Throwable) {
            Log.e("Failed to hook Play Store Application.attach: ${e.message}", e)
        }
    }

    private fun initializePlayStoreHooks(lpparam: LoadPackageParam, context: Context) {
        synchronized(Hook::class.java) {
            if (hasInitializedPlayStoreHooks) return
            hasInitializedPlayStoreHooks = true
        }

        Log.i("Initializing hooks in process: ${lpparam.processName}")

        // The user-visible update check runs in the main Play Store process. Do not restore
        // the cached hook or scan the full APK in recovery and other child processes.
        if (lpparam.processName == SpoofPolicy.TARGET_PACKAGE) {
            val decisionHookInstalled = hookSelfUpdateScheduler(lpparam, context)
            if (!decisionHookInstalled) {
                Log.e(
                    "No unique Play Store self-update scheduler was found; " +
                        "the primary hook is disabled and the legacy fallback remains opt-in"
                )
            }
        } else if (BuildConfig.DEBUG) {
            Log.i("Skipping the dynamic hook in child process: ${lpparam.processName}")
        }

        if (HookPreferenceReader.isLegacyPackageInfoFallbackEnabled()) {
            Log.i("Legacy PackageInfo fallback is enabled")
            hookLegacyPackageInfoFallback(lpparam)
        } else {
            Log.i("Legacy PackageInfo fallback is disabled")
        }
    }

    /**
     * Dynamically finds the outermost Play Store self-update scheduling method.
     *
     * Versions 18.8.16 and 41.0.28 use different internal comparisons, but both outer
     * methods return boolean and contain the two stable log fragments below. Their prefixes
     * and placeholders differ, so matching must use Contains. Returning false means that no
     * self-update was started or queued, which the settings UI reports as up to date.
     */
    private fun hookSelfUpdateScheduler(
        lpparam: LoadPackageParam,
        context: Context,
    ): Boolean {
        val installedVersionCode = try {
            context.packageManager
                .getPackageInfo(SpoofPolicy.TARGET_PACKAGE, 0)
                .longVersionCode
        } catch (e: Throwable) {
            Log.e("Failed to read the Play Store versionCode; persistent cache is disabled for this run", e)
            null
        }
        val cache = DynamicHookCache(context)

        if (installedVersionCode != null) {
            val cachedDescriptor = cache.readDescriptor(installedVersionCode)
            if (cachedDescriptor != null) {
                try {
                    val method = DexMethod(cachedDescriptor)
                        .getMethodInstance(lpparam.classLoader)
                    installSelfUpdateReplacement(method, cachedDescriptor, "cache")
                    Log.i(
                        "Restored self-update hook from cache: " +
                            "versionCode=$installedVersionCode; DexKit was not loaded"
                    )
                    return true
                } catch (e: Throwable) {
                    Log.e("Cached self-update hook is invalid; running a new search", e)
                    cache.clear()
                }
            }
        }

        return try {
            System.loadLibrary("dexkit")

            DexKitBridge.create(lpparam.appInfo.sourceDir).use { bridge ->
                val candidates = bridge.findMethod {
                    matcher {
                        returnType = "boolean"
                        usingStrings(
                            SelfUpdateHookPolicy.REQUIRED_STRING_FRAGMENTS,
                            StringMatchType.Contains,
                        )
                    }
                }

                if (!SelfUpdateHookPolicy.hasUniqueCandidate(candidates.size)) {
                    Log.e(
                        "Unexpected self-update scheduler candidate count: ${candidates.size}; " +
                            "expected exactly one"
                    )
                    return false
                }

                val target = candidates.single()
                val method = target.getMethodInstance(lpparam.classLoader)
                installSelfUpdateReplacement(method, target.descriptor, "DexKit")
                if (installedVersionCode != null) {
                    cache.writeDescriptor(installedVersionCode, target.descriptor)
                }
                true
            }
        } catch (e: Throwable) {
            Log.e("Failed to find or hook the self-update scheduler: ${e.message}", e)
            false
        }
    }

    private fun installSelfUpdateReplacement(
        method: Method,
        descriptor: String,
        source: String,
    ) {
        XposedBridge.hookMethod(
            method,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    if (!hasBlockedSelfUpdate) {
                        Log.i("Blocked Play Store self-update scheduling and returned up to date")
                        hasBlockedSelfUpdate = true
                    }
                    return SelfUpdateHookPolicy.NO_UPDATE_RESULT
                }
            }
        )
        Log.i("Hooked self-update scheduler via $source: $descriptor")
    }

    /**
     * Retains the old global PackageInfo spoof as an explicit compatibility fallback.
     */
    private fun hookLegacyPackageInfoFallback(lpparam: LoadPackageParam) {

        // Hook getPackageInfo(String, int)
        hookGetPackageInfo(
            lpparam,
            "getPackageInfo",
            arrayOf<Class<*>>(String::class.java, Int::class.javaPrimitiveType!!)
        )

        // Try getPackageInfo(String, PackageInfoFlags) on Android 13+.
        try {
            val flagsClass = XposedHelpers.findClass(
                "android.content.pm.PackageManager\$PackageInfoFlags",
                lpparam.classLoader
            )
            hookGetPackageInfo(
                lpparam,
                "getPackageInfo",
                arrayOf<Class<*>>(String::class.java, flagsClass)
            )
            
            // Hook getPackageInfoAsUser(String, PackageInfoFlags, int)
            hookGetPackageInfo(
                lpparam,
                "getPackageInfoAsUser",
                arrayOf<Class<*>>(String::class.java, flagsClass, Int::class.javaPrimitiveType!!)
            )
            
            // Hook getInstalledPackages(PackageInfoFlags)
            hookGetInstalledPackages(
                lpparam,
                "getInstalledPackages",
                arrayOf<Class<*>>(flagsClass)
            )

            // Hook getInstalledPackagesAsUser(PackageInfoFlags, int)
            hookGetInstalledPackages(
                lpparam,
                "getInstalledPackagesAsUser",
                arrayOf<Class<*>>(flagsClass, Int::class.javaPrimitiveType!!)
            )

        } catch (e:  Throwable) {
            Log.i("PackageManager\$PackageInfoFlags is unavailable or could not be hooked (SDK < 33?)")
        }

        // Hook getPackageInfoAsUser(String, int, int)
        hookGetPackageInfo(
            lpparam,
            "getPackageInfoAsUser",
            arrayOf<Class<*>>(String::class.java, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)
        )

        // Hook getInstalledPackages(int)
        hookGetInstalledPackages(
            lpparam,
            "getInstalledPackages",
            arrayOf<Class<*>>(Int::class.javaPrimitiveType!!)
        )

        // Hook getInstalledPackagesAsUser(int, int)
        hookGetInstalledPackages(
            lpparam,
            "getInstalledPackagesAsUser",
            arrayOf<Class<*>>(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)
        )

        // Hook getPackageInfo(VersionedPackage, int)
        val versionedPackageClassStr = "android.content.pm.VersionedPackage"
        try {
            val versionedClass = Class.forName(versionedPackageClassStr)
            hookGetPackageInfo(
                lpparam,
                "getPackageInfo",
                arrayOf<Class<*>>(versionedClass, Int::class.javaPrimitiveType!!)
            )

            // Try getPackageInfo(VersionedPackage, PackageInfoFlags) on Android 13+.
            try {
                val flagsClass = XposedHelpers.findClass(
                    "android.content.pm.PackageManager\$PackageInfoFlags",
                    lpparam.classLoader
                )
                hookGetPackageInfo(
                    lpparam,
                    "getPackageInfo",
                    arrayOf<Class<*>>(versionedClass, flagsClass)
                )
            } catch (e: Throwable) {
               // ignore
            }

        } catch (e: ClassNotFoundException) {
            Log.i("VersionedPackage is unavailable; skipping related hooks")
        }
    }

    /**
     * Hooks the getInstalledPackages method family.
     */
    private fun hookGetInstalledPackages(
        lpparam: LoadPackageParam,
        methodName: String,
        paramTypes: Array<Class<*>>
    ) {

        
        val methodHook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val resultList = param.result as? List<*> ?: return
                


                // Find Play Store in the returned package list.
                var found = false
                for (item in resultList) {
                    if (item is PackageInfo && SpoofPolicy.shouldSpoof(item.packageName)) {
                        if (BuildConfig.DEBUG) {
                            Log.i("[${lpparam.processName}] Found Play Store in $methodName list: ver=${item.versionName} (${item.longVersionCode})")
                        }
                        if (!hasHookedPlayStore) {
                            logVersion("Original version (List)", item)
                            modifyPackageInfo(item)
                            logVersion("Spoofed version (List)", item)
                            hasHookedPlayStore = true
                        } else {
                            modifyPackageInfo(item)
                        }
                        found = true
                        break 
                    }
                }
                if (!found && BuildConfig.DEBUG) {
                     Log.i("Play Store NOT found in $methodName result list")
                }
            }
        }

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                methodName,
                *paramTypes,
                methodHook
            )
            if (BuildConfig.DEBUG) {
                Log.i("Successfully hooked $methodName")
            }
        } catch (e: Throwable) {
             Log.e("Failed to hook $methodName: ${e.message}")
        }
    }

    /**
     * Shared hook implementation for multiple PackageManager signatures.
     */
    private fun hookGetPackageInfo(
        lpparam: LoadPackageParam,
        methodName: String,
        paramTypes: Array<Class<*>>
    ) {


        val methodHook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                // The first argument can be a String or a VersionedPackage.
                val pkgArg = param.args[0]
                val pkgName = when (pkgArg) {
                    is String -> pkgArg
                    else -> XposedHelpers.callMethod(pkgArg, "getPackageName") as? String ?: return
                }
                


                if (!SpoofPolicy.shouldSpoof(pkgName)) return

                // Read the original PackageInfo result.
                (param.result as? PackageInfo)?.let { pkgInfo ->
                    if (BuildConfig.DEBUG) {
                        Log.i("[${lpparam.processName}] Intercepted $pkgName info in $methodName: ver=${pkgInfo.versionName} (${pkgInfo.longVersionCode})")
                    }
                    if (!hasHookedPlayStore) {
                        // Log the original and spoofed values on the first interception.
                        Log.i("Catch method: $methodName")
                        logVersion("Original version", pkgInfo)
                        modifyPackageInfo(pkgInfo)
                        logVersion("Spoofed version", pkgInfo)
                        hasHookedPlayStore = true
                    } else {
                        // Later calls only update the version fields without repeating the log.
                        modifyPackageInfo(pkgInfo)
                    }
                    // Assign the modified object back to the hook result.
                    param.result = pkgInfo
                }
            }
        }

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                methodName,
                *paramTypes,
                methodHook
            )
            if (BuildConfig.DEBUG) {
                Log.i("Successfully hooked $methodName")
            }
        } catch (e: Throwable) {
             Log.e("Failed to hook $methodName: ${e.message}")
        }
    }

    /**
     * Logs PackageInfo version fields.
     */
    private fun logVersion(tagPrefix: String, packageInfo: PackageInfo) {
        val versionInfo = buildString {
            append("longVersionCode=").append(packageInfo.longVersionCode)
            append(", versionName=").append(packageInfo.versionName)
        }
        Log.i("$tagPrefix -> $versionInfo")
    }

    /**
     * Replaces PackageInfo version fields with the legacy spoof values.
     */
    private fun modifyPackageInfo(packageInfo: PackageInfo) {
        packageInfo.apply {
            longVersionCode = SpoofPolicy.VERSION_CODE
            versionName = SpoofPolicy.VERSION_NAME
        }
    }
}
