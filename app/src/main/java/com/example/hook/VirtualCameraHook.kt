package com.example.hook

import android.hardware.Camera
import android.os.Build
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

class VirtualCameraHook : IXposedHookLoadPackage {

    companion object {
        const val TAG = "VirtualCameraHook"
        val TARGET_PACKAGES = setOf("com.whatsapp", "com.imo.android.imoim")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!TARGET_PACKAGES.contains(lpparam.packageName)) {
            return
        }

        Log.d(TAG, "Target app loaded: ${lpparam.packageName}")

        try {
            // ১. ভার্সন ও ডিভাইস প্রপার্টি স্পুফিং
            spoofDeviceEnvironment(lpparam.classLoader)

            // ২. অ্যান্টি-ডিটেকশন ও এক্সপোজড ট্রেস হাইডিং
            hideXposedAndVirtualEnvironment(lpparam.classLoader)

            // ৩. ক্যামেরা ১ এপিআই হুকিং
            hookCamera1Preview(lpparam.classLoader)

            // ৪. ক্যামেরা ২ এপিআই হুকিং
            hookCamera2Session(lpparam.classLoader)

        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Hooking failed -> ${e.message}")
        }
    }

    /**
     * অ্যান্ড্রয়েড ভার্সন ও সিস্টেম বিল্ড স্পুফ করার মেথড
     */
    private fun spoofDeviceEnvironment(classLoader: ClassLoader) {
        try {
            // আসল ডিভাইসের মতো পিক্সেল বা স্যামসাং মডেলের ফেক তথ্য রিটার্ন করা
            XposedHelpers.setStaticObjectField(Build::class.java, "MANUFACTURER", "Google")
            XposedHelpers.setStaticObjectField(Build::class.java, "MODEL", "Pixel 8 Pro")
            XposedHelpers.setStaticObjectField(Build::class.java, "BRAND", "google")
            XposedHelpers.setStaticObjectField(Build::class.java, "DEVICE", "husky")
            XposedHelpers.setStaticIntField(Build.VERSION::class.java, "SDK_INT", 34) // Android 14 SDK
            XposedHelpers.setStaticObjectField(Build.VERSION::class.java, "RELEASE", "14")

            Log.d(TAG, "Device environment successfully spoofed to Google Pixel 8 Pro (Android 14)")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to spoof device environment: ${e.message}")
        }
    }

    /**
     * এক্সপোজড ফ্রেমওয়ার্ক এবং ভার্চুয়াল স্পেস হাইড করার লজিক
     */
    private fun hideXposedAndVirtualEnvironment(classLoader: ClassLoader) {
        try {
            // File.exists() হুক করে Xposed বা রুট ফাইল চেক ব্লক করা
            XposedHelpers.findAndHookMethod(
                File::class.java,
                "exists",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val file = param.thisObject as File
                        val path = file.absolutePath.lowercase()
                        if (path.contains("xposed") || path.contains("su") || path.contains("magisk")) {
                            param.result = false // ফাইল নেই বলে উত্তর দেওয়া
                        }
                    }
                }
            )

            // StackTrace থেকে Xposed এর ক্লাসের নাম ফিল্টার করা
            XposedHelpers.findAndHookMethod(
                Throwable::class.java,
                "getStackTrace",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val result = param.result as? Array<StackTraceElement> ?: return
                        val filteredList = result.filterNot { element ->
                            element.className.contains("de.robv.android.xposed") ||
                            element.className.contains("VirtualXposed")
                        }.toTypedArray()
                        param.result = filteredList
                    }
                }
            )

            Log.d(TAG, "Xposed & Virtual space traces hidden successfully.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to set anti-detection hooks: ${e.message}")
        }
    }

    private fun hookCamera1Preview(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.hardware.Camera",
                classLoader,
                "addCallbackBuffer",
                ByteArray::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        super.beforeHookedMethod(param)
                        val buffer = param.args[0] as? ByteArray
                        if (buffer != null) {
                            // কাস্টম ভার্চুয়াল ক্যামেরা ফ্রেম ইনজেকশন
                        }
                    }
                }
            )
            Log.d(TAG, "Camera 1 preview hook initialized.")
        } catch (e: Throwable) {
            Log.e(TAG, "Error hooking Camera 1: ${e.message}")
        }
    }

    private fun hookCamera2Session(classLoader: ClassLoader) {
        try {
            Log.d(TAG, "Camera 2 session hook structure prepared.")
        } catch (e: Throwable) {
            Log.e(TAG, "Error hooking Camera 2: ${e.message}")
        }
    }
}
