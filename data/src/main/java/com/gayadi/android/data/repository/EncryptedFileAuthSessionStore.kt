package com.gayadi.android.data.repository

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.model.AuthUser
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

/** Persists one rotating token pair encrypted by a non-exportable Android Keystore key. */
class EncryptedFileAuthSessionStore(
    private val file: File,
) : AuthSessionStore {
    override fun load(): AuthSession? {
        if (!file.exists()) return null
        return runCatching {
            val envelope = JSONObject(file.readText())
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateKey(),
                    GCMParameterSpec(TAG_LENGTH_BITS, envelope.getString(IV).decodeBase64()),
                )
            }
            JSONObject(String(cipher.doFinal(envelope.getString(CIPHERTEXT).decodeBase64()), Charsets.UTF_8))
                .toSession()
        }.getOrElse {
            clear()
            null
        }
    }

    override fun save(session: AuthSession) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val encrypted = cipher.doFinal(session.toJson().toString().toByteArray(Charsets.UTF_8))
        val envelope = JSONObject()
            .put(IV, cipher.iv.encodeBase64())
            .put(CIPHERTEXT, encrypted.encodeBase64())

        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(envelope.toString())
        check(temporary.renameTo(file) || run {
            file.delete()
            temporary.renameTo(file)
        }) { "인증 세션을 저장하지 못했습니다." }
    }

    override fun clear() {
        if (file.exists()) file.delete()
        File(file.parentFile, "${file.name}.tmp").delete()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "gayadi-auth-session-v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
        const val IV = "iv"
        const val CIPHERTEXT = "ciphertext"
    }
}

private fun AuthSession.toJson(): JSONObject = JSONObject()
    .put("accessToken", accessToken)
    .put("tokenType", tokenType)
    .put("expiresIn", expiresInSeconds)
    .put("refreshToken", refreshToken)
    .put("refreshExpiresIn", refreshExpiresInSeconds)
    .put("issuedAt", issuedAtEpochSeconds)
    .put("user", JSONObject()
        .put("id", user.id)
        .put("nickname", user.nickname)
        .put("email", user.email)
        .put("introduction", user.introduction)
        .put("profileImageUrl", user.profileImageUrl)
        .put("status", user.status)
        .put("lastLoginAt", user.lastLoginAt)
        .put("createdAt", user.createdAt)
        .put("updatedAt", user.updatedAt))

private fun JSONObject.toSession(): AuthSession {
    val user = getJSONObject("user")
    return AuthSession(
        accessToken = getString("accessToken"),
        tokenType = getString("tokenType"),
        expiresInSeconds = getLong("expiresIn"),
        refreshToken = getString("refreshToken"),
        refreshExpiresInSeconds = getLong("refreshExpiresIn"),
        issuedAtEpochSeconds = getLong("issuedAt"),
        user = AuthUser(
            id = user.getLong("id"),
            nickname = user.nullableString("nickname"),
            email = user.getString("email"),
            introduction = user.nullableString("introduction"),
            profileImageUrl = user.nullableString("profileImageUrl"),
            status = user.nullableString("status"),
            lastLoginAt = user.nullableString("lastLoginAt"),
            createdAt = user.nullableString("createdAt"),
            updatedAt = user.nullableString("updatedAt"),
        ),
    )
}

private fun JSONObject.nullableString(name: String): String? =
    takeUnless { isNull(name) }?.optString(name)?.takeIf(String::isNotBlank)

private fun ByteArray.encodeBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
private fun String.decodeBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
