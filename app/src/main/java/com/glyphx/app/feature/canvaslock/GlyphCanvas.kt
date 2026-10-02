package com.glyphx.app.feature.canvaslock

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.glyphx.app.core.gesture.GlyphStroke
import com.glyphx.app.core.gesture.TouchPoint
import com.glyphx.app.core.ui.theme.NeonPurple
import com.glyphx.app.core.ui.theme.NeonPurpleBright

enum class CanvasGlowMode {
    DEFAULT,
    VERIFYING,
    SUCCESS,
    ERROR
}

@Composable
fun GlyphCanvas(
    modifier: Modifier = Modifier,
    glowMode: CanvasGlowMode = CanvasGlowMode.DEFAULT,
    primaryColor: Color = NeonPurple,
    brightColor: Color = NeonPurpleBright,
    enabled: Boolean = true,
    onStrokesChanged: (List<GlyphStroke>) -> Unit = {},
    onStrokeFinished: (List<GlyphStroke>) -> Unit = {}
) {
    val strokes = remember { mutableStateListOf<GlyphStroke>() }
    var currentStrokePoints by remember { mutableStateOf<List<TouchPoint>>(emptyList()) }
    var activeFingerPosition by remember { mutableStateOf<Offset?>(null) }

    val strokeWidthPx = 10f
    val glowRadiusPx = 28f

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown()
                    val startPt = TouchPoint(down.position.x, down.position.y)
                    val activePoints = mutableListOf(startPt)
                    currentStrokePoints = activePoints.toList()
                    activeFingerPosition = down.position

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.pressed) {
                            val newPt = TouchPoint(change.position.x, change.position.y)
                            activePoints.add(newPt)
                            currentStrokePoints = activePoints.toList()
                            activeFingerPosition = change.position
                            change.consume()
                        }
                    } while (event.changes.any { it.pressed })

                    // Stroke ended
                    if (activePoints.size >= 2) {
                        val completedStroke = GlyphStroke(activePoints.toList())
                        strokes.add(completedStroke)
                        onStrokesChanged(strokes.toList())
                        onStrokeFinished(strokes.toList())
                    }
                    currentStrokePoints = emptyList()
                    activeFingerPosition = null
                }
            }
    ) {
        val (coreColor, glowColor) = when (glowMode) {
            CanvasGlowMode.DEFAULT -> Pair(brightColor, primaryColor.copy(alpha = 0.55f))
            CanvasGlowMode.VERIFYING -> Pair(Color(0xFF60A5FA), Color(0xFF3B82F6).copy(alpha = 0.65f))
            CanvasGlowMode.SUCCESS -> Pair(Color(0xFF34D399), Color(0xFF10B981).copy(alpha = 0.75f))
            CanvasGlowMode.ERROR -> Pair(Color(0xFFF87171), Color(0xFFEF4444).copy(alpha = 0.75f))
        }

        // Draw all completed strokes + current active stroke
        val allStrokes = if (currentStrokePoints.isNotEmpty()) {
            strokes.toList() + GlyphStroke(currentStrokePoints)
        } else {
            strokes.toList()
        }

        for (stroke in allStrokes) {
            if (stroke.points.size < 2) continue

            val path = Path().apply {
                val first = stroke.points[0]
                moveTo(first.x, first.y)
                for (i in 1 until stroke.points.size) {
                    val p = stroke.points[i]
                    lineTo(p.x, p.y)
                }
            }

            // 1. Draw outer neon glow using Native Blur Filter
            drawIntoCanvas { canvas ->
                val glowPaint = Paint().apply {
                    color = glowColor.toArgb()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeWidthPx + glowRadiusPx
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                    maskFilter = BlurMaskFilter(glowRadiusPx, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.nativeCanvas.drawPath(path.asAndroidPath(), glowPaint)
            }

            // 2. Draw sharp bright luminous core
            drawPath(
                path = path,
                color = coreColor,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Draw glowing luminous particle orb at active finger tip
        activeFingerPosition?.let { pos ->
            drawCircle(
                color = glowColor,
                radius = 24f,
                center = pos
            )
            drawCircle(
                color = Color.White,
                radius = 7f,
                center = pos
            )
        }
    }
}
