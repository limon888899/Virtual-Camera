package com.example.ui.screens

import com.example.ui.theme.StudioColors

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun Android15SecurityScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var refreshKey by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Check permissions dynamically
    val hasOverlay = remember(refreshKey) { Settings.canDrawOverlays(context) }
    val hasMic = remember(refreshKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val hasNotification = remember(refreshKey) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isIgnoringBattery = remember(refreshKey) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshKey++
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // OS Banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioColors.Accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Security Shield",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Android permission status",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioColors.Text
                        )
                        Text(
                            text = "Device permissions and background-service settings",
                            fontSize = 11.sp,
                            color = StudioColors.Accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StudioColors.Ink,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "This screen checks the permissions used by the local media assistant: overlay, microphone, notifications, and battery settings.",
                        fontSize = 11.sp,
                        color = StudioColors.Text.copy(alpha = 0.8f),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Permissions Checklist Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Permission & System Status",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioColors.Text
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 1. Overlay
                PermissionRow(
                    title = "Display Over Other Apps (Overlay)",
                    subtitle = "Required for the floating control bubble",
                    isGranted = hasOverlay,
                    onGrant = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Microphone
                PermissionRow(
                    title = "Record Audio (Microphone)",
                    subtitle = "Required for Real-time Lip-Sync & Noise Shield",
                    isGranted = hasMic,
                    onGrant = {
                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Notifications
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionRow(
                        title = "Post Notifications",
                        subtitle = "Required to keep the foreground assistant visible",
                        isGranted = hasNotification,
                        onGrant = {
                            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 4. Battery Optimization
                PermissionRow(
                    title = "Unrestricted Battery Background Access",
                        subtitle = "Helps Android keep the local assistant running",
                    isGranted = isIgnoringBattery,
                    onGrant = {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Manifest & Architecture Verification Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = StudioColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Manifest FGS Configuration",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioColors.Text
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StudioColors.Ink,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "<service\n" +
                                "    android:name=\".service.VirtualCameraService\"\n" +
                                "    android:exported=\"false\"\n" +
                                "    android:foregroundServiceType=\"microphone\" />",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = StudioColors.Blue,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = StudioColors.AccentSoft,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Configuration reflects the local media assistant",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudioColors.AccentSoft
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioColors.Ink)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) StudioColors.AccentSoft else StudioColors.Warning,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = StudioColors.Text
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = StudioColors.Text.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        if (!isGranted) {
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Accent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant", fontSize = 11.sp)
            }
        } else {
            Text(
                text = "Active",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StudioColors.AccentSoft
            )
        }
    }
}
