package com.glyphx.app.feature.canvaslock

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.glyphx.app.core.gesture.GlyphNormalizer
import com.glyphx.app.core.gesture.GlyphStroke
import com.glyphx.app.core.security.BiometricPromptHelper
import com.glyphx.app.core.security.GlyphSecurityManager
import com.glyphx.app.core.ui.theme.ErrorRed
import com.glyphx.app.core.ui.theme.NeonCyan
import com.glyphx.app.core.ui.theme.NeonPurple
import com.glyphx.app.core.ui.theme.NeonPurpleBright
import com.glyphx.app.core.ui.theme.SuccessGreen
import com.glyphx.app.core.ui.theme.TextMuted
import com.glyphx.app.core.ui.theme.TextPrimary
import com.glyphx.app.core.ui.theme.TextSecondary
import com.glyphx.app.core.ui.theme.VoidBlack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LockUiState {
    IDLE,
    DRAWING,
    VERIFYING,
    SUCCESS,
    ERROR
}

@Composable
fun CanvasLockScreen(
    securityManager: GlyphSecurityManager,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lockoutRemaining by securityManager.lockoutRemainingSeconds.collectAsState()
    val failedAttempts by securityManager.failedAttempts.collectAsState()

    var uiState by remember { mutableStateOf(LockUiState.IDLE) }
    var currentStrokes by remember { mutableStateOf<List<GlyphStroke>>(emptyList()) }
    var canvasKey by remember { mutableIntStateOf(0) } // Incremented to clear canvas

    // Clock state
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now)
            delay(1000L)
        }
    }

    // Auto-trigger biometric on start if biometric backup is enabled
    LaunchedEffect(Unit) {
        if (securityManager.isBiometricEnabled() && lockoutRemaining == 0) {
            val activity = context as? FragmentActivity
            if (activity != null && BiometricPromptHelper.isBiometricAvailable(context)) {
                BiometricPromptHelper.showPrompt(
                    activity = activity,
                    onSuccess = {
                        securityManager.unlockViaBiometric()
                        onUnlocked()
                    },
                    onError = {
                        // User can still draw glyph
                    }
                )
            }
        }
    }

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
                    val effect = VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 80), -1)
                    vibrator.vibrate(effect)
                } else {
                    val effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (isError) 200L else 50L)
            }
        } catch (_: Exception) {}
    }

    fun verifyDrawnStrokes(strokes: List<GlyphStroke>) {
        if (lockoutRemaining > 0) return

        scope.launch {
            uiState = LockUiState.VERIFYING
            delay(500L) // Pulsing animation time

            val candidate = GlyphNormalizer.normalize(strokes)
            if (candidate == null) {
                uiState = LockUiState.ERROR
                triggerVibration(true)
                delay(900L)
                canvasKey++
                currentStrokes = emptyList()
                uiState = LockUiState.IDLE
                return@launch
            }

            val result = securityManager.verifyCandidate(candidate)
            if (result.matched) {
                uiState = LockUiState.SUCCESS
                triggerVibration(false)
                delay(800L)
                onUnlocked()
            } else {
                uiState = LockUiState.ERROR
                triggerVibration(true)
                delay(1000L)
                canvasKey++
                currentStrokes = emptyList()
                uiState = LockUiState.IDLE
            }
        }
    }

    // Glow Mode for Canvas
    val glowMode = when (uiState) {
        LockUiState.IDLE, LockUiState.DRAWING -> CanvasGlowMode.DEFAULT
        LockUiState.VERIFYING -> CanvasGlowMode.VERIFYING
        LockUiState.SUCCESS -> CanvasGlowMode.SUCCESS
        LockUiState.ERROR -> CanvasGlowMode.ERROR
    }

    // Pulse animation for Verifying state
    val infiniteTransition = rememberInfiniteTransition(label = "verifying")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        // Background radial glow
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.Center)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            when (uiState) {
                                LockUiState.SUCCESS -> SuccessGreen.copy(alpha = 0.22f)
                                LockUiState.ERROR -> ErrorRed.copy(alpha = 0.22f)
                                LockUiState.VERIFYING -> NeonCyan.copy(alpha = 0.20f)
                                else -> NeonPurple.copy(alpha = 0.15f)
                            },
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Verifying Circular Orbital Ring (mockup panel 3)
        if (uiState == LockUiState.VERIFYING) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.Center)
                    .scale(pulseScale)
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(NeonCyan, NeonPurple, NeonPurpleBright, NeonCyan)
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Main Drawing Canvas (fill entire screen)
        key(canvasKey) {
            GlyphCanvas(
                modifier = Modifier.fillMaxSize(),
                glowMode = glowMode,
                enabled = (uiState == LockUiState.IDLE || uiState == LockUiState.DRAWING) && lockoutRemaining == 0,
                onStrokesChanged = { updated ->
                    currentStrokes = updated
                    if (uiState == LockUiState.IDLE && updated.isNotEmpty()) {
                        uiState = LockUiState.DRAWING
                    }
                },
                onStrokeFinished = { updated ->
                    // Auto-trigger verification upon stroke release
                    verifyDrawnStrokes(updated)
                }
            )
        }

        // Header: GlyphX Logo, Clock, Subtitle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stylized GlyphX Logo
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(NeonPurple, NeonCyan)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "G",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Time & Date
            Text(
                text = currentTimeString.ifEmpty { "9:41" },
                fontSize = 54.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                letterSpacing = (-1.0).sp
            )
            Text(
                text = currentDateString.ifEmpty { "Tue, 23 Sep" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Instructions / Lockout message
            if (lockoutRemaining > 0) {
                Text(
                    text = "Too many attempts. Retry in ${lockoutRemaining}s",
                    fontSize = 14.sp,
                    color = ErrorRed,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = when (uiState) {
                        LockUiState.IDLE, LockUiState.DRAWING -> "Draw your glyph to unlock"
                        LockUiState.VERIFYING -> "Verifying..."
                        LockUiState.SUCCESS -> "Welcome back"
                        LockUiState.ERROR -> "Try again"
                    },
                    fontSize = 15.sp,
                    color = when (uiState) {
                        LockUiState.SUCCESS -> SuccessGreen
                        LockUiState.ERROR -> ErrorRed
                        LockUiState.VERIFYING -> NeonCyan
                        else -> TextSecondary
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // State Badge (Checkmark on Success / Cross on Error)
        AnimatedVisibility(
            visible = uiState == LockUiState.SUCCESS || uiState == LockUiState.ERROR,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (uiState == LockUiState.SUCCESS) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (uiState == LockUiState.SUCCESS) SuccessGreen else ErrorRed,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState == LockUiState.SUCCESS) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (uiState == LockUiState.SUCCESS) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState == LockUiState.SUCCESS) "Welcome back" else "Try again",
                    color = if (uiState == LockUiState.SUCCESS) SuccessGreen else ErrorRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Bottom Controls Bar (Biometric bottom left, Clear button, Overflow bottom right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Biometric button (panel 1 bottom left)
            IconButton(
                onClick = {
                    val activity = context as? FragmentActivity
                    if (activity != null && BiometricPromptHelper.isBiometricAvailable(context)) {
                        BiometricPromptHelper.showPrompt(
                            activity = activity,
                            onSuccess = {
                                securityManager.unlockViaBiometric()
                                onUnlocked()
                            },
                            onError = {}
                        )
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF161824), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Unlock",
                    tint = TextSecondary,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Clear button if user made a mistake while drawing
            if (currentStrokes.isNotEmpty() && uiState == LockUiState.DRAWING) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E2235))
                        .clickable {
                            canvasKey++
                            currentStrokes = emptyList()
                            uiState = LockUiState.IDLE
                        }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Clear",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            // More Options Icon
            IconButton(
                onClick = { /* subtle placeholder */ },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF161824), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Options",
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
