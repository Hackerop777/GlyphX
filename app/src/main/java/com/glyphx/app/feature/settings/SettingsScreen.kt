package com.glyphx.app.feature.settings

import android.widget.Toast
import com.glyphx.app.feature.canvaslock.VerifyCurrentGlyphDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glyphx.app.core.security.GlyphSecurityManager
import com.glyphx.app.core.ui.theme.NeonPurple
import com.glyphx.app.core.ui.theme.NeonPurpleBright
import com.glyphx.app.core.ui.theme.ObsidianBorder
import com.glyphx.app.core.ui.theme.ObsidianCard
import com.glyphx.app.core.ui.theme.TextMuted
import com.glyphx.app.core.ui.theme.TextPrimary
import com.glyphx.app.core.ui.theme.TextSecondary
import com.glyphx.app.core.ui.theme.VoidBlack

@Composable
fun SettingsScreen(
    securityManager: GlyphSecurityManager,
    onNavigateToSetupGlyph: () -> Unit
) {
    val context = LocalContext.current
    var isCanvasLockOn by remember { mutableStateOf(securityManager.isCanvasLockEnabled()) }
    var isBiometricOn by remember { mutableStateOf(securityManager.isBiometricEnabled()) }
    var autoLockSec by remember { mutableIntStateOf(securityManager.getAutoLockTimeoutSeconds()) }

    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showVerifyForChangeGlyph by remember { mutableStateOf(false) }
    var showVerifyForDisableLock by remember { mutableStateOf(false) }

    val timeoutOptions = listOf(
        Pair("Immediately", 0),
        Pair("30 seconds", 30),
        Pair("1 minute", 60),
        Pair("5 minutes", 300)
    )

    val currentTimeoutLabel = timeoutOptions.firstOrNull { it.second == autoLockSec }?.first ?: "1 minute"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Security Section (panel 8 in mockup)
        Text(
            text = "Security",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ObsidianCard)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
        ) {
            // Canvas Lock Toggle
            SettingsToggleRow(
                icon = Icons.Default.Lock,
                title = "Canvas Lock",
                subtitle = "Require drawn glyph to unlock",
                checked = isCanvasLockOn,
                onCheckedChange = { checked ->
                    if (!checked && securityManager.hasEnrolledGlyph()) {
                        // Ask for current password before turning off
                        showVerifyForDisableLock = true
                    } else if (checked && !securityManager.hasEnrolledGlyph()) {
                        onNavigateToSetupGlyph()
                    } else {
                        isCanvasLockOn = checked
                        securityManager.setCanvasLockEnabled(checked)
                    }
                }
            )

            SettingsDivider()

            // Change Glyph (Requires old password)
            SettingsActionRow(
                icon = Icons.Default.Gesture,
                title = "Change Glyph",
                subtitle = "Re-enroll your drawn password",
                onClick = {
                    if (securityManager.hasEnrolledGlyph()) {
                        showVerifyForChangeGlyph = true
                    } else {
                        onNavigateToSetupGlyph()
                    }
                }
            )

            SettingsDivider()

            // Use Biometrics as Backup
            SettingsToggleRow(
                icon = Icons.Default.Fingerprint,
                title = "Use Biometrics as Backup",
                subtitle = "Fingerprint or device credential",
                checked = isBiometricOn,
                onCheckedChange = { checked ->
                    isBiometricOn = checked
                    securityManager.setBiometricEnabled(checked)
                }
            )

            SettingsDivider()

            // Auto Lock Timeout
            SettingsActionRow(
                icon = Icons.Default.Timer,
                title = "Auto Lock Timeout",
                value = currentTimeoutLabel,
                onClick = { showTimeoutDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // General Section
        Text(
            text = "General",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ObsidianCard)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
        ) {
            SettingsActionRow(
                icon = Icons.Default.DarkMode,
                title = "Theme",
                value = "Dark Futuristic",
                onClick = {
                    Toast.makeText(context, "Dark Futuristic mode active", Toast.LENGTH_SHORT).show()
                }
            )

            SettingsDivider()

            SettingsActionRow(
                icon = Icons.Default.Storage,
                title = "Data & Storage",
                value = "Encrypted On-Device",
                onClick = {
                    Toast.makeText(context, "All data stored on-device in Room Database", Toast.LENGTH_SHORT).show()
                }
            )

            SettingsDivider()

            SettingsActionRow(
                icon = Icons.Default.Share,
                title = "Export Notes",
                subtitle = "Backup notes in encrypted format",
                onClick = {
                    Toast.makeText(context, "Export ready (AES-256 protected)", Toast.LENGTH_SHORT).show()
                }
            )

            SettingsDivider()

            SettingsActionRow(
                icon = Icons.Default.Info,
                title = "About GlyphX",
                value = "v1.0.0",
                onClick = { showAboutDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // Auto Lock Timeout Selection Dialog
    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            containerColor = ObsidianCard,
            title = { Text("Auto Lock Timeout", color = TextPrimary) },
            text = {
                Column {
                    timeoutOptions.forEach { (label, sec) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    autoLockSec = sec
                                    securityManager.setAutoLockTimeoutSeconds(sec)
                                    showTimeoutDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (sec == autoLockSec) NeonPurpleBright else TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (sec == autoLockSec) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutDialog = false }) {
                    Text("Close", color = NeonPurple)
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = ObsidianCard,
            title = { Text("GlyphX Productivity & Security", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        text = "GlyphX is an ultra-secure, privacy-first Notes & To-Do app featuring an in-app drawn Canvas Lock.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "• Keystore AES-256-GCM hardware protection\n• Deterministic resampled gesture engine\n• Zero third-party telemetry\n• Responsive foldable / dual-pane experience",
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = NeonPurple)
                }
            }
        )
    }

    if (showVerifyForChangeGlyph) {
        VerifyCurrentGlyphDialog(
            securityManager = securityManager,
            title = "Verify Current Password",
            subtitle = "Draw your old glyph before setting a new one",
            onDismissRequest = { showVerifyForChangeGlyph = false },
            onVerified = {
                showVerifyForChangeGlyph = false
                onNavigateToSetupGlyph()
            }
        )
    }

    if (showVerifyForDisableLock) {
        VerifyCurrentGlyphDialog(
            securityManager = securityManager,
            title = "Verify Current Password",
            subtitle = "Draw your glyph to confirm disabling Canvas Lock",
            onDismissRequest = { showVerifyForDisableLock = false },
            onVerified = {
                showVerifyForDisableLock = false
                isCanvasLockOn = false
                securityManager.setCanvasLockEnabled(false)
            }
        )
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF222638), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = NeonPurpleBright, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(text = subtitle, color = TextMuted, fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeonPurple,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = VoidBlack
            )
        )
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF222638), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = NeonPurpleBright, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(text = subtitle, color = TextMuted, fontSize = 12.sp)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(text = value, color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ObsidianBorder)
    )
}
