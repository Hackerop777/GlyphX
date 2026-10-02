package com.glyphx.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidKeystoreHelperTest {

    private val helper = AndroidKeystoreHelper()

    @Test
    fun testEncryptionAndDecryptionRoundTrip() {
        val sampleSecret = "{\"strokeCount\":2,\"aspectRatio\":1.05}"
        val encrypted = helper.encrypt(sampleSecret)

        assertNotNull(encrypted)
        val decrypted = helper.decrypt(encrypted)

        assertEquals("Decrypted plaintext must match original", sampleSecret, decrypted)
    }

    @Test
    fun testTamperedCiphertextReturnsNull() {
        val sampleSecret = "sensitive_data"
        val encrypted = helper.encrypt(sampleSecret)

        val tampered = encrypted.dropLast(4) + "AAAA"
        val decrypted = helper.decrypt(tampered)

        assertNull("Decryption of corrupted ciphertext must return null", decrypted)
    }
}
