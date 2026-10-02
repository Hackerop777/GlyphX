package com.glyphx.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.glyphx.app.core.ui.theme.GlyphXTheme
import com.glyphx.app.core.ui.theme.VoidBlack
import com.glyphx.app.feature.canvaslock.CanvasLockScreen
import com.glyphx.app.feature.canvaslock.CanvasLockSetupScreen
import com.glyphx.app.ui.adaptive.ResponsiveAppShell

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as GlyphXApplication
        val securityManager = app.securityManager
        val notesViewModel = app.notesViewModel
        val todoViewModel = app.todoViewModel

        setContent {
            GlyphXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VoidBlack
                ) {
                    val isUnlocked by securityManager.isUnlocked.collectAsState()
                    var isSettingUpGlyph by remember { mutableStateOf(false) }

                    AnimatedContent(
                        targetState = Pair(isUnlocked, isSettingUpGlyph),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "app_root_transition"
                    ) { (unlocked, settingUp) ->
                        if (!unlocked) {
                            CanvasLockScreen(
                                securityManager = securityManager,
                                onUnlocked = {
                                    // Security manager updates isUnlocked to true
                                }
                            )
                        } else if (settingUp) {
                            CanvasLockSetupScreen(
                                securityManager = securityManager,
                                onNavigateBack = { isSettingUpGlyph = false },
                                onSetupComplete = { isSettingUpGlyph = false }
                            )
                        } else {
                            ResponsiveAppShell(
                                notesViewModel = notesViewModel,
                                todoViewModel = todoViewModel,
                                securityManager = securityManager,
                                onNavigateToSetupGlyph = { isSettingUpGlyph = true }
                            )
                        }
                    }
                }
            }
        }
    }
}
