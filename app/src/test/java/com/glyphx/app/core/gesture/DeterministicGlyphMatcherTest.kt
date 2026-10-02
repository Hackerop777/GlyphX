package com.glyphx.app.core.gesture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class DeterministicGlyphMatcherTest {

    private val matcher = DeterministicGlyphMatcher(threshold = 0.74f)

    private fun createCircleGesture(centerX: Float, centerY: Float, radius: Float): List<GlyphStroke> {
        val points = mutableListOf<TouchPoint>()
        val steps = 40
        for (i in 0..steps) {
            val angle = (2.0 * Math.PI * i / steps).toFloat()
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            points.add(TouchPoint(x, y))
        }
        return listOf(GlyphStroke(points))
    }

    private fun createTriangleGesture(originX: Float, originY: Float, size: Float): List<GlyphStroke> {
        val points = mutableListOf<TouchPoint>()
        for (i in 0..15) {
            val t = i / 15f
            points.add(TouchPoint(originX + t * size, originY))
        }
        for (i in 0..15) {
            val t = i / 15f
            points.add(TouchPoint(originX + size - t * (size / 2f), originY + t * size))
        }
        for (i in 0..15) {
            val t = i / 15f
            points.add(TouchPoint(originX + (size / 2f) * (1f - t), originY + size * (1f - t)))
        }
        return listOf(GlyphStroke(points))
    }

    private fun createStarGesture(
        centerX: Float,
        centerY: Float,
        radius: Float,
        startVertex: Int = 0,
        reverse: Boolean = false,
        wobble: Float = 0f
    ): List<GlyphStroke> {
        val angles = (0 until 5).map { (Math.PI / 2.0 + it * 2.0 * Math.PI / 5.0).toFloat() }
        val order = mutableListOf(0, 2, 4, 1, 3, 0)
        if (reverse) {
            order.reverse()
        }
        val startIdx = order.indexOf(startVertex % 5)
        val pathOrder = (order.subList(startIdx, order.size - 1) + order.subList(0, startIdx) + listOf(order[startIdx]))

        val points = mutableListOf<TouchPoint>()
        for (i in 0 until pathOrder.size - 1) {
            val v1 = pathOrder[i]
            val v2 = pathOrder[i + 1]
            val a1 = angles[v1]
            val a2 = angles[v2]
            val x1 = centerX + radius * cos(a1)
            val y1 = centerY + radius * sin(a1)
            val x2 = centerX + radius * cos(a2)
            val y2 = centerY + radius * sin(a2)

            for (s in 0..10) {
                val t = s / 10f
                val wx = (wobble * 4f * sin(s.toDouble())).toFloat()
                val wy = (wobble * 4f * cos(s.toDouble())).toFloat()
                points.add(TouchPoint(x1 + t * (x2 - x1) + wx, y1 + t * (y2 - y1) + wy))
            }
        }
        return listOf(GlyphStroke(points))
    }

    @Test
    fun testSelfMatchScoresHigh() {
        val circle = createCircleGesture(300f, 400f, 150f)
        val template = GlyphNormalizer.normalize(circle)
        assertNotNull(template)

        val result = matcher.match(template!!, template)
        assertTrue("Self match should pass, score was ${result.score}", result.matched)
        assertTrue("Score should be > 0.95", result.score > 0.95f)
    }

    @Test
    fun testScaleAndTranslationInvariance() {
        val enrolled = GlyphNormalizer.normalize(createCircleGesture(200f, 200f, 80f))!!
        val candidateStrokes = createCircleGesture(600f, 800f, 220f)
        val candidate = GlyphNormalizer.normalize(candidateStrokes)!!

        val result = matcher.match(candidate, enrolled)
        assertTrue("Scaled and shifted circle should match, score: ${result.score}", result.matched)
        assertTrue("Score should be >= 0.85", result.score >= 0.85f)
    }

    @Test
    fun testStarGestureInvariantToStartingVertexAndDirection() {
        // Enrolled star starting at top point (vertex 0)
        val enrolledStrokes = createStarGesture(400f, 500f, 200f, startVertex = 0)
        val enrolled = GlyphNormalizer.normalize(enrolledStrokes)!!

        // Candidate 1: Star drawn starting at bottom-left point (vertex 2) with slight human hand jitter
        val candidateStrokesVertex2 = createStarGesture(350f, 450f, 180f, startVertex = 2, wobble = 0.5f)
        val candidate1 = GlyphNormalizer.normalize(candidateStrokesVertex2)!!
        val result1 = matcher.match(candidate1, enrolled)
        assertTrue("Star starting at different vertex must match! Score: ${result1.score}", result1.matched)
        assertTrue("Score must be >= 0.80", result1.score >= 0.80f)

        // Candidate 2: Star drawn in reverse direction (counter-clockwise)
        val candidateStrokesReverse = createStarGesture(420f, 520f, 210f, startVertex = 3, reverse = true, wobble = 0.3f)
        val candidate2 = GlyphNormalizer.normalize(candidateStrokesReverse)!!
        val result2 = matcher.match(candidate2, enrolled)
        assertTrue("Star drawn in reverse direction must match! Score: ${result2.score}", result2.matched)
        assertTrue("Score must be >= 0.80", result2.score >= 0.80f)

        // Candidate 3: Circle vs Star must still be rejected!
        val circleStrokes = createCircleGesture(400f, 500f, 200f)
        val circle = GlyphNormalizer.normalize(circleStrokes)!!
        val resultCircle = matcher.match(circle, enrolled)
        assertFalse("Circle must not match Star! Score: ${resultCircle.score}", resultCircle.matched)
        assertTrue("Score must be < 0.65", resultCircle.score < 0.65f)
    }

    @Test
    fun testDifferentShapesRejected() {
        val circle = GlyphNormalizer.normalize(createCircleGesture(300f, 300f, 120f))!!
        val triangle = GlyphNormalizer.normalize(createTriangleGesture(200f, 200f, 200f))!!

        val result = matcher.match(triangle, circle)
        assertFalse("Triangle should not match circle, score: ${result.score}", result.matched)
        assertTrue("Score should be < 0.70", result.score < 0.70f)
    }

    @Test
    fun testTooShortGestureRejected() {
        val tap = listOf(
            GlyphStroke(listOf(TouchPoint(100f, 100f), TouchPoint(102f, 102f)))
        )
        val template = GlyphNormalizer.normalize(tap)
        assertNull("Tap shorter than minimum length must return null template", template)
    }
}
