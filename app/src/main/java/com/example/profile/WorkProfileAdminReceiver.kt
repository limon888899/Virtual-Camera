package com.example.profile

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build

class WorkProfileAdminReceiver : DeviceAdminReceiver() {
    override fun onProfileProvisioningComplete(context: Context, intent: android.content.Intent) {
        super.onProfileProvisioningComplete(context, intent)

        val admin = ComponentName(context, WorkProfileAdminReceiver::class.java)
        val policyManager = context.getSystemService(DevicePolicyManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            policyManager.setProfileName(admin, "Live Media Assistant Space")
        }

        val packageName = intent
            .getBundleExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
            ?.getString(PENDING_PACKAGE)
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { policyManager.installExistingPackage(admin, packageName) }
                .onSuccess {
                }
        }
    }

    companion object {
        const val PREFERENCES = "work_profile"
        const val PENDING_PACKAGE = "pending_package"
    }
}
