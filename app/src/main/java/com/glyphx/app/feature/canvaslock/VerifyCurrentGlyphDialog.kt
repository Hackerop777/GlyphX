package com.glyphx.app.feature.canvaslock

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.glyphx.app.core.gesture.GlyphNormalizer
import com.glyphx.app.core.gesture.GlyphStroke
import com.glyphx.app.core.security.BiometricPromptHelper
import com.glyphx.app.core.security.GlyphSecurityManager
import com.glyphx.app.core.ui.theme.ErrorRed
import com.glyphx.app.core.ui.theme.NeonPurple
import com.glyphx.app.core.ui.theme.ObsidianBorder
import com.glyphx.app.core.ui.theme.ObsidianCard
import com.glyphx.app.core.ui.theme.SuccessGreen
import com.glyphx.app.core.ui.theme.TextMuted
import com.glyphx.app.core.ui.theme.TextPrimary
import com.glyphx.app.core.ui.theme.TextSecondary
import com.glyphx.app.core.ui.theme.VoidBlack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VerifyCurrentGlyphDialog(
    securityManager: GlyphSecurityManager,
    title: String = "Verify Current Glyph",
    subtitle: String = "Draw your existing glyph to continue",
    onDismissRequest: () -> Unit,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentStrokes by remember { mutableStateOf<List<GlyphStroke>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var canvasKey by remember { mutableIntStateOf(0) }
    var glowMode by remember { mutableStateOf(CanvasGlowMode.DEFAULT) }

    fun triggerVibration(isError: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isError) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 80), -1))
                } else {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (isError) 200L else 50L)
            }
        } catch (_: Exception) {}
    }

    fun verifyStrokes(strokes: List<GlyphStroke>) {
        if (strokes.isEmpty()) return

        scope.launch {
            glowMode = CanvasGlowMode.VERIFYING
            val candidate = GlyphNormalizer.normalize(strokes)
            if (candidate == null) {
                errorMessage = "Gesture too short. Try again."
                glowMode = CanvasGlowMode.ERROR
                triggerVibration(true)
                delay(700L)
                canvasKey++
                currentStrokes = emptyList()
                glowMode = CanvasGlowMode.DEFAULT
                return@launch
            }

            val enrolled = securityManager.getEnrolledGlyph()
            if (enrolled == null) {
                // No password set, auto-verify
                onVerified()
                return@launch
            }

            val result = securityManager.verifyCandidate(candidate)
            if (result.matched) {
                glowMode = CanvasGlowMode.SUCCESS
                triggerVibration(false)
                delay(500L)
                onVerified()
            } else {
                errorMessage = "Incorrect glyph. Please try again."
                glowMode = CanvasGlowMode.ERROR
                triggerVibration(true)
                delay(800L)
                canvasKey++
                currentStrokes = emptyList()
                glowMode = CanvasGlowMode.DEFAULT
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.95f))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }

                    if (securityManager.isBiometricEnabled()) {
                        IconButton(
                            onClick = {
                                val activity = context as? FragmentActivity
                                if (activity != null && BiometricPromptHelper.isBiometricAvailable(context)) {
                                    BiometricPromptHelper.showPrompt(
                                        activity = activity,
                                        title = "Verify Identity",
                                        subtitle = "Use biometric to proceed",
                                        onSuccess = { onVerified() },
                                        onError = { /* fallback to canvas */ }
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF1E2235), shape = CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = "Biometric Verification",
                                tint = NeonPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                // Drawing Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(ObsidianCard)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
                ) {
                    key(canvasKey) {
                        GlyphCanvas(
                            modifier = Modifier.fillMaxSize(),
                            glowMode = glowMode,
                            onStrokesChanged = { updated ->
                                currentStrokes = updated
                                errorMessage = null
                            },
                            onStrokeFinished = { updated ->
                                verifyStrokes(updated)
                            }
                        )
                    }

                    // Reset button overlay
                    IconButton(
                        onClick = {
                            canvasKey++
                            currentStrokes = emptyList()
                            errorMessage = null
                            glowMode = CanvasGlowMode.DEFAULT
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .background(Color(0xFF1E2235), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cancel button
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Cancel", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
