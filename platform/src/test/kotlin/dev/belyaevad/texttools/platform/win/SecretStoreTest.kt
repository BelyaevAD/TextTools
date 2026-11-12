package dev.belyaevad.texttools.platform.win

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** DPAPI and stand-in protectors. Secret values are not printed. */
class SecretStoreTest {
    @Test
    fun missingDirectoryIsAnErrorAndWritesNoFile() {
        val parent = Files.createTempDirectory("texttools-p5-absent-")
        try {
            val root = parent.resolve("absent")
            val secrets = userSecrets(root, XorProtector)
            assertFailsWith<SecretStoreException> {
                secrets.put(SecretSlot.ApiKey, token())
            }
            if (Files.exists(root)) error("missing store created a directory")
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun plaintextProtectorIsRefused() {
        val root = Files.createTempDirectory("texttools-p5-plain-")
        try {
            val secrets = userSecrets(root, IdentityProtector)
            assertFailsWith<SecretStoreException> {
                secrets.put(SecretSlot.ProxyUrl, token())
            }
            if (Files.exists(root.resolve("proxy-url.dpapi"))) error("plaintext was written")
            if (Files.exists(root.resolve("proxy-url.txt"))) error("password file was written")
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun xorRoundTripDoesNotStoreThePlaintext() {
        val root = Files.createTempDirectory("texttools-p5-xor-")
        try {
            val secrets = userSecrets(root, XorProtector)
            val apiKey = token()
            val proxy = token()
            secrets.put(SecretSlot.ApiKey, apiKey)
            secrets.put(SecretSlot.ProxyUrl, proxy)
            assertSameSecret(secrets.get(SecretSlot.ApiKey), apiKey)
            assertSameSecret(secrets.get(SecretSlot.ProxyUrl), proxy)
            assertCiphertext(root.resolve("api-key.dpapi"), apiKey)
            assertCiphertext(root.resolve("proxy-url.dpapi"), proxy)
            if (Files.exists(root.resolve("api-key.txt"))) error("password file was written")
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun dpapiRoundTripDoesNotStoreThePlaintext() {
        if (!windowsHost()) return
        val root = Files.createTempDirectory("texttools-p5-dpapi-")
        try {
            val secrets = userSecrets(root, DpapiProtector)
            val apiKey = token()
            val proxy = token()
            secrets.put(SecretSlot.ApiKey, apiKey)
            secrets.put(SecretSlot.ProxyUrl, proxy)
            assertSameSecret(secrets.get(SecretSlot.ApiKey), apiKey)
            assertSameSecret(secrets.get(SecretSlot.ProxyUrl), proxy)
            assertCiphertext(root.resolve("api-key.dpapi"), apiKey)
            assertCiphertext(root.resolve("proxy-url.dpapi"), proxy)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun aForeignBlobIsNotReadAsText() {
        if (!windowsHost()) return
        val root = Files.createTempDirectory("texttools-p5-foreign-")
        try {
            Files.write(root.resolve("api-key.dpapi"), byteArrayOf(1, 2, 3, 4))
            val secrets = userSecrets(root, DpapiProtector)
            assertFailsWith<SecretStoreException> {
                secrets.get(SecretSlot.ApiKey)
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun missingSlotIsNull() {
        val root = Files.createTempDirectory("texttools-p5-empty-")
        try {
            assertNull(userSecrets(root, XorProtector).get(SecretSlot.ApiKey))
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}

private object XorProtector : SecretProtector {
    override fun protect(plain: ByteArray): ByteArray =
        ByteArray(plain.size) { index ->
            (plain[index].toInt() xor 0x5A).toByte()
        }

    override fun unprotect(blob: ByteArray): ByteArray = protect(blob)
}

private object IdentityProtector : SecretProtector {
    override fun protect(plain: ByteArray): ByteArray = plain.copyOf()

    override fun unprotect(blob: ByteArray): ByteArray = blob.copyOf()
}

private fun token(): String = "p5-" + java.util.UUID.randomUUID()

private fun assertSameSecret(
    actual: String?,
    expected: String,
) {
    if (actual != expected) error("secret round trip failed")
}

private fun assertCiphertext(
    path: Path,
    secret: String,
) {
    val stored = Files.readAllBytes(path)
    if (stored.toString(StandardCharsets.ISO_8859_1).contains(secret)) {
        error("ciphertext file contains the plaintext secret")
    }
}
