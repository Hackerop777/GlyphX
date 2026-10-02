package com.glyphx.app.feature.canvaslock

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glyphx.app.core.gesture.DeterministicGlyphMatcher
import com.glyphx.app.core.gesture.GlyphNormalizer
import com.glyphx.app.core.gesture.GlyphStroke
import com.glyphx.app.core.gesture.GlyphTemplate
import com.glyphx.app.core.security.GlyphSecurityManager
import com.glyphx.app.core.ui.theme.ErrorRed
import com.glyphx.app.core.ui.theme.NeonPurple
import com.glyphx.app.core.ui.theme.NeonPurpleBright
import com.glyphx.app.core.ui.theme.ObsidianCard
import com.glyphx.app.core.ui.theme.SuccessGreen
import com.glyphx.app.core.ui.theme.TextMuted
import com.glyphx.app.core.ui.theme.TextPrimary
import com.glyphx.app.core.ui.theme.TextSecondary
import com.glyphx.app.core.ui.theme.VoidBlack

enum class SetupStep {
    DRAW_NEW,
    CONFIRM_NEW,
    SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasLockSetupScreen(
    securityManager: GlyphSecurityManager,
    onNavigateBack: () -> Unit,
    onSetupComplete: () -> Unit
) {
    var step by remember { mutableStateOf(SetupStep.DRAW_NEW) }
    var firstAttemptTemplate by remember { mutableStateOf<GlyphTemplate?>(null) }
    var currentStrokes by remember { mutableStateOf<List<GlyphStroke>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var canvasKey by remember { mutableIntStateOf(0) }

    val matcher = remember { DeterministicGlyphMatcher(threshold = 0.72f) }

    fun resetDrawing() {
        currentStrokes = emptyList()
        canvasKey++
        errorMessage = null
    }

    fun handleNext() {
        if (currentStrokes.isEmpty()) {
            errorMessage = "Please draw your glyph first"
            return
        }

        val template = GlyphNormalizer.normalize(currentStrokes)
        if (template == null) {
            errorMessage = "Gesture is too short or simple. Draw a more distinct shape."
            return
        }

        when (step) {
            SetupStep.DRAW_NEW -> {
                firstAttemptTemplate = template
                step = SetupStep.CONFIRM_NEW
                resetDrawing()
            }
            SetupStep.CONFIRM_NEW -> {
                val first = firstAttemptTemplate
                if (first == null) {
                    step = SetupStep.DRAW_NEW
                    resetDrawing()
                    return
                }

                val matchResult = matcher.match(template, first)
                if (matchResult.matched) {
                    val combinedTemplate = first.copy(secondarySamplePoints = template.normalizedPoints)
                    val saved = securityManager.enrollGlyph(combinedTemplate)
                    if (saved) {
                        step = SetupStep.SUCCESS
                    } else {
                        errorMessage = "Failed to save to secure Keystore"
                    }
                } else {
                    errorMessage = "Glyphs do not match. Try redrawing to confirm."
                    resetDrawing()
                }
            }
            SetupStep.SUCCESS -> {
                onSetupComplete()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Canvas Lock Setup",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VoidBlack
                )
            )
        },
        containerColor = VoidBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Step Indicator & Guidance
            Text(
                text = when (step) {
                    SetupStep.DRAW_NEW -> "Draw your custom glyph"
                    SetupStep.CONFIRM_NEW -> "Confirm your glyph"
                    SetupStep.SUCCESS -> "Glyph Lock Activated"
                },
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (step) {
                    SetupStep.DRAW_NEW -> "Draw a continuous or multi-stroke pattern that you will use to unlock GlyphX."
                    SetupStep.CONFIRM_NEW -> "Draw the exact same gesture again to confirm your canvas password."
                    SetupStep.SUCCESS -> "Your custom gesture has been normalized and encrypted into the Android Keystore."
                },
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = ErrorRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Canvas Area
            if (step != SetupStep.SUCCESS) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(ObsidianCard)
                        .border(1.dp, Color(0xFF2E334D), RoundedCornerShape(24.dp))
                ) {
                    key(canvasKey) {
                        GlyphCanvas(
                            modifier = Modifier.fillMaxSize(),
                            glowMode = CanvasGlowMode.DEFAULT,
                            onStrokesChanged = { updated ->
                                currentStrokes = updated
                                errorMessage = null
                            }
                        )
                    }

                    // Reset button overlay
                    IconButton(
                        onClick = { resetDrawing() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(38.dp)
                            .background(Color(0xFF1E2235), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear Canvas",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Success screen icon
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(SuccessGreen.copy(alpha = 0.15f), shape = CircleShape)
                                .border(2.dp, SuccessGreen, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Hardware Keystore Secured",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Normalized template encrypted using AES-256-GCM",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (step == SetupStep.CONFIRM_NEW) {
                    OutlinedButton(
                        onClick = {
                            step = SetupStep.DRAW_NEW
                            firstAttemptTemplate = null
                            resetDrawing()
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        )
                    ) {
                        Text("Start Over")
                    }
                }

                Button(
                    onClick = { handleNext() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (step == SetupStep.SUCCESS) SuccessGreen else NeonPurple
                    )
                ) {
                    Text(
                        text = when (step) {
                            SetupStep.DRAW_NEW -> "Continue"
                            SetupStep.CONFIRM_NEW -> "Confirm & Save"
                            SetupStep.SUCCESS -> "Done"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
