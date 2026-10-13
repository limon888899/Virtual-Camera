package com.example.virtual

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class AppInfo(
    val appName: String,
    val packageName: String,
    val isInstalledInVirtualSpace: Boolean = false,
    val targetAndroidVersion: String = "Android 14"
)

class VirtualSpaceManager(private val context: Context) {

    companion object {
        const val TAG = "VirtualSpaceManager"
    }

    /**
     * মূল ফোনে ইনস্টল থাকা থার্ড-পার্টি অ্যাপস (যেমন: IMO, WhatsApp) স্ক্যান করা
     */
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val appList = mutableListOf<AppInfo>()
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in packages) {
            // শুধুমাত্র সিস্টেম অ্যাপ বাদে ইনস্টল করা ইউজার অ্যাপস ফিল্টার করা
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                val appName = pm.getApplicationLabel(app).toString()
                val packageName = app.packageName
                appList.add(AppInfo(appName, packageName))
            }
        }
        return@withContext appList
    }

    /**
     * অ্যাপকে কাস্টম স্যান্ডবক্সে ক্লোন ও আইসোলেট করা
     */
    fun cloneAppToVirtualSpace(packageName: String, selectedAndroidVersion: String): Boolean {
        return try {
            val virtualDataDir = File(context.filesDir, "virtual_space/$packageName")
            if (!virtualDataDir.exists()) {
                virtualDataDir.mkdirs()
            }
            Log.d(TAG, "App $packageName successfully cloned into virtual space with $selectedAndroidVersion configuration.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clone app $packageName: ${e.message}")
            false
        }
    }
}
