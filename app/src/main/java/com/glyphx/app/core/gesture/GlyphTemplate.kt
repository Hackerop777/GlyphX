package com.glyphx.app.core.gesture

import kotlinx.serialization.Serializable

@Serializable
data class DirectionVector(
    val dx: Float,
    val dy: Float
)

@Serializable
data class GlyphTemplate(
    val normalizedPoints: List<TouchPoint>,
    val secondarySamplePoints: List<TouchPoint>? = null,
    val strokeCount: Int = 1,
    val strokeBreakIndices: List<Int> = emptyList(),
    val aspectRatio: Float = 1.0f,
    val directionalVectors: List<DirectionVector> = emptyList()
)
