package com.mymod.playspoofer.preferences

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import androidx.annotation.Keep
import com.mymod.playspoofer.xposed.SpoofPolicy

@Keep
class FallbackPreferenceProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != PreferenceContract.METHOD_GET_LEGACY_PACKAGE_INFO_FALLBACK) {
            return super.call(method, arg, extras)
        }

        enforceAllowedCaller()
        val appContext = requireNotNull(context).applicationContext
        return Bundle().apply {
            putBoolean(
                PreferenceContract.RESULT_ENABLED,
                FallbackPreferences.isLegacyPackageInfoFallbackEnabled(appContext),
            )
        }
    }

    private fun enforceAllowedCaller() {
        val callingUid = Binder.getCallingUid()
        if (callingUid == Process.myUid()) return

        val callerPackages = context?.packageManager?.getPackagesForUid(callingUid).orEmpty()
        if (SpoofPolicy.TARGET_PACKAGE !in callerPackages) {
            throw SecurityException("Caller is not allowed to read hook preferences")
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
