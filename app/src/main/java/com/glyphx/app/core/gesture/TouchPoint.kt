package com.glyphx.app.core.gesture

import kotlinx.serialization.Serializable

@Serializable
data class TouchPoint(
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun distanceTo(other: TouchPoint): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

@Serializable
data class GlyphStroke(
    val points: List<TouchPoint> = emptyList()
) {
    fun length(): Float {
        if (points.size < 2) return 0f
        var total = 0f
        for (i in 1 until points.size) {
            total += points[i - 1].distanceTo(points[i])
        }
        return total
    }
}
