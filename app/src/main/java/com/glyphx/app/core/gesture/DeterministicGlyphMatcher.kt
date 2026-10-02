package com.glyphx.app.core.gesture

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DeterministicGlyphMatcher(
    private val threshold: Float = DEFAULT_THRESHOLD
) : GlyphRecognizer {

    companion object {
        const val DEFAULT_THRESHOLD = 0.74f
    }

    override fun match(candidate: GlyphTemplate, enrolled: GlyphTemplate): MatchResult {
        // Test candidate against primary enrolled sample
        val primaryScore = scoreSample(candidate, enrolled.normalizedPoints, enrolled.aspectRatio)

        // If enrolled has a secondary sample (from confirmation step), test against that too!
        val secondaryScore = enrolled.secondarySamplePoints?.let { secondSample ->
            scoreSample(candidate, secondSample, enrolled.aspectRatio)
        } ?: 0.0f

        val bestScore = max(primaryScore, secondaryScore)
        val isMatched = bestScore >= threshold

        return MatchResult(
            matched = isMatched,
            score = bestScore,
            reason = if (isMatched) "Match accepted (score: $bestScore)" else "Score $bestScore below threshold $threshold"
        )
    }

    private fun scoreSample(
        candidate: GlyphTemplate,
        enrolledPoints: List<TouchPoint>,
        enrolledAspectRatio: Float
    ): Float {
        val candPoints = candidate.normalizedPoints
        if (candPoints.isEmpty() || enrolledPoints.isEmpty()) return 0.0f

        // 1. Aspect Ratio compatibility
        val minRatio = min(candidate.aspectRatio, enrolledAspectRatio)
        val maxRatio = max(candidate.aspectRatio, enrolledAspectRatio)
        val ratioScore = if (maxRatio > 0f) (minRatio / maxRatio) else 1.0f

        // Bounded aspect penalty: extreme squashing penalized, reasonable variations allowed
        val aspectModifier = when {
            ratioScore >= 0.45f -> 1.0f
            ratioScore >= 0.30f -> 0.85f
            else -> 0.65f
        }

        // 2. Bidirectional Chamfer Shape Distance
        // Invariant to starting vertex, stroke order, and drawing direction
        val chamferDistance = calculateBidirectionalChamfer(candPoints, enrolledPoints)
        // A matching shape (like star, circle, signature) typically has Chamfer < 0.14 in [-1, 1] space
        val shapeScore = max(0.0f, 1.0f - (chamferDistance / 0.32f))

        // 3. Cyclic & Reverse Path Alignment
        // Invariant to starting position along the loop/path
        val cyclicDistance = calculateBestCyclicDistance(candPoints, enrolledPoints)
        val cyclicScore = max(0.0f, 1.0f - (cyclicDistance / 0.40f))

        // 4. Combined Similarity
        // If cyclic alignment is strong (e.g. same loop drawn from another vertex), factor it in.
        val baseScore = max(shapeScore, (0.60f * shapeScore) + (0.40f * cyclicScore))
        val finalScore = (baseScore * aspectModifier).coerceIn(0.0f, 1.0f)

        return finalScore
    }

    /**
     * Computes the symmetric bidirectional Chamfer distance between two point clouds.
     * D = (1/|P| * sum_p min_q |p - q| + 1/|Q| * sum_q min_p |q - p|) / 2
     */
    private fun calculateBidirectionalChamfer(pList: List<TouchPoint>, qList: List<TouchPoint>): Float {
        var pToQTotal = 0f
        for (p in pList) {
            var minDist = Float.MAX_VALUE
            for (q in qList) {
                val d = p.distanceTo(q)
                if (d < minDist) {
                    minDist = d
                }
            }
            pToQTotal += minDist
        }
        val pToQAvg = pToQTotal / pList.size

        var qToPTotal = 0f
        for (q in qList) {
            var minDist = Float.MAX_VALUE
            for (p in pList) {
                val d = q.distanceTo(p)
                if (d < minDist) {
                    minDist = d
                }
            }
            qToPTotal += minDist
        }
        val qToPAvg = qToPTotal / qList.size

        return (pToQAvg + qToPAvg) / 2.0f
    }

    /**
     * Computes the minimum average point distance across all cyclic shifts
     * and reverse directions.
     */
    private fun calculateBestCyclicDistance(pList: List<TouchPoint>, qList: List<TouchPoint>): Float {
        val n = min(pList.size, qList.size)
        if (n == 0) return Float.MAX_VALUE

        val step = max(1, n / 32) // Sample every 1 or 2 offsets for fast real-time responsiveness
        var bestDist = Float.MAX_VALUE

        for (k in 0 until n step step) {
            var fwdDist = 0f
            var revDist = 0f

            for (i in 0 until n) {
                val fwdIndex = (i + k) % n
                val revIndex = (n - 1 - ((i + k) % n))

                fwdDist += pList[fwdIndex].distanceTo(qList[i])
                revDist += pList[revIndex].distanceTo(qList[i])
            }

            val curMin = min(fwdDist / n, revDist / n)
            if (curMin < bestDist) {
                bestDist = curMin
            }
        }

        return bestDist
    }
}
