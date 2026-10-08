package com.example.ui.screens

import com.example.ui.theme.StudioColors

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppInfo
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun VirtualSpaceScreen(
    installedApps: List<AppInfo>,
    clonedApps: List<AppInfo>,
    cloneStatus: String,
    onCloneApp: (AppInfo) -> Unit,
    onRemoveClonedApp: (AppInfo) -> Unit,
    onLaunchClonedApp: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioColors.Surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SpaceDashboard, contentDescription = null, tint = StudioColors.Warning)
                    Spacer(Modifier.width(10.dp))
                    Text("Virtual Space", color = StudioColors.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    "Select one app. Android will create one real managed profile and install the selected app there. The first setup requires system confirmation.",
                    color = StudioColors.Text.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.size(8.dp))
                Text(cloneStatus, color = StudioColors.Accent, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.size(16.dp))
        Text("Selected app", color = StudioColors.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.size(8.dp))
        if (clonedApps.isEmpty()) {
            Text("No app selected", color = StudioColors.Text.copy(alpha = 0.6f), fontSize = 13.sp)
        } else {
            clonedApps.forEach { app ->
                SelectedAppRow(app, { onLaunchClonedApp(app) }, { onRemoveClonedApp(app) })
            }
        }

        Spacer(Modifier.size(18.dp))
        Text("Installed apps", color = StudioColors.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.size(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 72.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(installedApps.filter { !it.isCloned }, key = { it.packageName }) { app ->
                Column(
                    modifier = Modifier.clickable(role = Role.Button) { onCloneApp(app) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)).background(StudioColors.SurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        app.icon?.let {
                            Image(rememberDrawablePainter(it), contentDescription = app.appName, modifier = Modifier.size(42.dp))
                        } ?: Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = StudioColors.Text)
                    }
                    Spacer(Modifier.size(5.dp))
                    Text(app.appName, color = StudioColors.Text.copy(alpha = 0.85f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SelectedAppRow(app: AppInfo, onLaunch: () -> Unit, onRemove: () -> Unit) {
    Surface(color = StudioColors.Surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            app.icon?.let { Image(rememberDrawablePainter(it), contentDescription = app.appName, modifier = Modifier.size(38.dp)) }
            Spacer(Modifier.width(10.dp))
            Text(app.appName, color = StudioColors.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Button(onClick = onLaunch) { Icon(Icons.Default.PlayArrow, contentDescription = "Launch") }
            Button(onClick = onRemove) { Text("Refresh", fontSize = 10.sp) }
        }
    }
}
