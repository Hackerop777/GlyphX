package com.glyphx.app.core.security

import android.content.Context
import android.content.SharedPreferences
import com.glyphx.app.core.gesture.DeterministicGlyphMatcher
import com.glyphx.app.core.gesture.GlyphRecognizer
import com.glyphx.app.core.gesture.GlyphTemplate
import com.glyphx.app.core.gesture.MatchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GlyphSecurityManager(
    private val context: Context,
    private val keystoreHelper: AndroidKeystoreHelper = AndroidKeystoreHelper(),
    private val matcher: GlyphRecognizer = DeterministicGlyphMatcher()
) {

    private val prefs: SharedPreferences = context.getSharedPreferences("glyphx_security_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_ENCRYPTED_GLYPH = "enc_glyph_template"
        private const val KEY_CANVAS_LOCK_ENABLED = "canvas_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT_SEC = "auto_lock_timeout_sec"

        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L
    }

    private val _isUnlocked = MutableStateFlow(!isCanvasLockEnabled() || !hasEnrolledGlyph())
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _lockoutRemainingSeconds = MutableStateFlow(0)
    val lockoutRemainingSeconds: StateFlow<Int> = _lockoutRemainingSeconds.asStateFlow()

    private var lockoutEndTimeMs: Long = 0L
    private var lastBackgroundTimeMs: Long = 0L

    init {
        // If canvas lock is disabled or no glyph enrolled, app is automatically unlocked
        if (!isCanvasLockEnabled() || !hasEnrolledGlyph()) {
            _isUnlocked.value = true
        } else {
            _isUnlocked.value = false
        }
    }

    fun isCanvasLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_CANVAS_LOCK_ENABLED, false)
    }

    fun setCanvasLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CANVAS_LOCK_ENABLED, enabled).apply()
        if (!enabled) {
            _isUnlocked.value = true
        } else if (hasEnrolledGlyph()) {
            _isUnlocked.value = false
        }
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getAutoLockTimeoutSeconds(): Int {
        return prefs.getInt(KEY_AUTO_LOCK_TIMEOUT_SEC, 60) // default 1 minute
    }

    fun setAutoLockTimeoutSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT_SEC, seconds).apply()
    }

    fun hasEnrolledGlyph(): Boolean {
        return prefs.getString(KEY_ENCRYPTED_GLYPH, null) != null
    }

    fun enrollGlyph(template: GlyphTemplate): Boolean {
        return try {
            val templateJson = json.encodeToString(template)
            val encrypted = keystoreHelper.encrypt(templateJson)
            prefs.edit()
                .putString(KEY_ENCRYPTED_GLYPH, encrypted)
                .putBoolean(KEY_CANVAS_LOCK_ENABLED, true)
                .apply()
            _isUnlocked.value = true
            _failedAttempts.value = 0
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getEnrolledGlyph(): GlyphTemplate? {
        val encrypted = prefs.getString(KEY_ENCRYPTED_GLYPH, null) ?: return null
        val decryptedJson = keystoreHelper.decrypt(encrypted) ?: return null
        return try {
            json.decodeFromString<GlyphTemplate>(decryptedJson)
        } catch (e: Exception) {
            null
        }
    }

    fun verifyCandidate(candidate: GlyphTemplate): MatchResult {
        val currentTime = System.currentTimeMillis()
        if (currentTime < lockoutEndTimeMs) {
            val remainingSec = ((lockoutEndTimeMs - currentTime) / 1000).toInt() + 1
            return MatchResult(false, 0f, "Locked out. Try again in $remainingSec s")
        }

        val enrolled = getEnrolledGlyph()
            ?: return MatchResult(false, 0f, "No enrolled glyph found")

        val result = matcher.match(candidate, enrolled)
        if (result.matched) {
            _failedAttempts.value = 0
            _isUnlocked.value = true
        } else {
            val newFailures = _failedAttempts.value + 1
            _failedAttempts.value = newFailures
            if (newFailures >= MAX_FAILED_ATTEMPTS) {
                triggerLockout()
            }
        }
        return result
    }

    fun unlockViaBiometric(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime < lockoutEndTimeMs) return false

        _failedAttempts.value = 0
        _isUnlocked.value = true
        return true
    }

    fun lockApp() {
        if (isCanvasLockEnabled() && hasEnrolledGlyph()) {
            _isUnlocked.value = false
        }
    }

    fun recordAppBackgrounded() {
        lastBackgroundTimeMs = System.currentTimeMillis()
    }

    fun evaluateForegroundSecurity() {
        if (!isCanvasLockEnabled() || !hasEnrolledGlyph()) {
            _isUnlocked.value = true
            return
        }

        val timeoutSec = getAutoLockTimeoutSeconds()
        if (timeoutSec == 0) {
            // Immediate lock
            _isUnlocked.value = false
        } else if (lastBackgroundTimeMs > 0L) {
            val elapsedSec = (System.currentTimeMillis() - lastBackgroundTimeMs) / 1000
            if (elapsedSec >= timeoutSec) {
                _isUnlocked.value = false
            }
        }
    }

    private fun triggerLockout() {
        lockoutEndTimeMs = System.currentTimeMillis() + LOCKOUT_DURATION_MS
        _lockoutRemainingSeconds.value = (LOCKOUT_DURATION_MS / 1000).toInt()

        CoroutineScope(Dispatchers.Default).launch {
            while (System.currentTimeMillis() < lockoutEndTimeMs) {
                val remaining = ((lockoutEndTimeMs - System.currentTimeMillis()) / 1000).toInt() + 1
                _lockoutRemainingSeconds.value = remaining
                delay(1000L)
            }
            _lockoutRemainingSeconds.value = 0
            _failedAttempts.value = 0
        }
    }
}
