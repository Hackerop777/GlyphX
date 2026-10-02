package com.glyphx.app.core.gesture

import kotlin.math.max
import kotlin.math.sqrt

object GlyphNormalizer {

    const val DEFAULT_RESAMPLE_COUNT = 64
    const val MINIMUM_PATH_LENGTH_PX = 80f

    /**
     * Converts a collection of raw drawn strokes into a standardized GlyphTemplate.
     * Returns null if strokes are too short or invalid.
     */
    fun normalize(strokes: List<GlyphStroke>, targetPointCount: Int = DEFAULT_RESAMPLE_COUNT): GlyphTemplate? {
        val validStrokes = strokes.filter { it.points.size >= 2 }
        if (validStrokes.isEmpty()) return null

        val totalLength = validStrokes.sumOf { it.length().toDouble() }.toFloat()
        if (totalLength < MINIMUM_PATH_LENGTH_PX) return null

        // 1. Resample strokes into targetPointCount equidistant points
        val resampled = resampleStrokes(validStrokes, targetPointCount)
        if (resampled.points.isEmpty()) return null

        // 2. Compute Centroid
        var sumX = 0f
        var sumY = 0f
        for (pt in resampled.points) {
            sumX += pt.x
            sumY += pt.y
        }
        val centroidX = sumX / resampled.points.size
        val centroidY = sumY / resampled.points.size

        // 3. Center points
        val centered = resampled.points.map { pt ->
            TouchPoint(pt.x - centroidX, pt.y - centroidY, pt.timestamp)
        }

        // 4. Compute bounding box and scale
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (pt in centered) {
            if (pt.x < minX) minX = pt.x
            if (pt.x > maxX) maxX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.y > maxY) maxY = pt.y
        }

        val width = max(maxX - minX, 1e-4f)
        val height = max(maxY - minY, 1e-4f)
        val maxDim = max(width, height)
        val aspectRatio = width / height

        // 5. Scale to normalized [-1.0, 1.0]
        val halfDim = maxDim / 2f
        val normalized = centered.map { pt ->
            TouchPoint(pt.x / halfDim, pt.y / halfDim, pt.timestamp)
        }

        // 6. Compute direction vectors
        val directions = mutableListOf<DirectionVector>()
        for (i in 0 until normalized.size - 1) {
            val p1 = normalized[i]
            val p2 = normalized[i + 1]
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val mag = sqrt(dx * dx + dy * dy)
            if (mag > 1e-5f) {
                directions.add(DirectionVector(dx / mag, dy / mag))
            } else {
                directions.add(DirectionVector(0f, 0f))
            }
        }
        if (directions.isNotEmpty()) {
            directions.add(directions.last())
        }

        return GlyphTemplate(
            normalizedPoints = normalized,
            strokeCount = validStrokes.size,
            strokeBreakIndices = resampled.breakIndices,
            aspectRatio = aspectRatio,
            directionalVectors = directions
        )
    }

    private data class ResampleResult(
        val points: List<TouchPoint>,
        val breakIndices: List<Int>
    )

    private fun resampleStrokes(strokes: List<GlyphStroke>, n: Int): ResampleResult {
        val totalLength = strokes.sumOf { it.length().toDouble() }.toFloat()
        if (totalLength <= 0f) return ResampleResult(emptyList(), emptyList())

        val interval = totalLength / (n - 1)
        var accumulatedDistance = 0f
        val outputPoints = mutableListOf<TouchPoint>()
        val breakIndices = mutableListOf<Int>()

        // Add start of first stroke
        outputPoints.add(strokes[0].points[0])

        var currentPointIndex = 0

        for (strokeIdx in strokes.indices) {
            val stroke = strokes[strokeIdx]
            val pts = stroke.points
            if (strokeIdx > 0) {
                breakIndices.add(outputPoints.size)
            }

            for (i in 1 until pts.size) {
                val p1 = pts[i - 1]
                val p2 = pts[i]
                var d = p1.distanceTo(p2)

                if (accumulatedDistance + d >= interval) {
                    var currP1 = p1
                    while (accumulatedDistance + d >= interval && outputPoints.size < n) {
                        val t = (interval - accumulatedDistance) / d
                        val nx = currP1.x + t * (p2.x - currP1.x)
                        val ny = currP1.y + t * (p2.y - currP1.y)
                        val newPt = TouchPoint(nx, ny)
                        outputPoints.add(newPt)
                        d -= (interval - accumulatedDistance)
                        currP1 = newPt
                        accumulatedDistance = 0f
                    }
                    accumulatedDistance = d
                } else {
                    accumulatedDistance += d
                }
            }
        }

        // Fill remaining points if rounding left us short
        while (outputPoints.size < n) {
            outputPoints.add(strokes.last().points.last())
        }

        return ResampleResult(outputPoints.take(n), breakIndices)
    }
}
