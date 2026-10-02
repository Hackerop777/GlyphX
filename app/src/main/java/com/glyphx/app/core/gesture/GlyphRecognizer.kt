package com.glyphx.app.core.gesture

data class MatchResult(
    val matched: Boolean,
    val score: Float,
    val reason: String = ""
)

interface GlyphRecognizer {
    fun match(candidate: GlyphTemplate, enrolled: GlyphTemplate): MatchResult
}
