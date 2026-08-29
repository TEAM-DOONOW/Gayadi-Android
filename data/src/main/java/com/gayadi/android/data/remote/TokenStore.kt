package com.gayadi.android.data.remote

import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Minimal persistence contract used to attach an access token to API requests. */
interface TokenStore {
    fun readAccessToken(): String?

    fun writeAccessToken(accessToken: String)

    fun clearAccessToken()
}

/** Stores the token in a file supplied from the application's private files directory. */
class FileTokenStore(private val file: File) : TokenStore {
    @Synchronized
    override fun readAccessToken(): String? =
        file.takeIf(File::isFile)
            ?.readText(Charsets.UTF_8)
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    @Synchronized
    override fun writeAccessToken(accessToken: String) {
        require(accessToken.isNotBlank()) { "Access token must not be blank." }

        val target = file.absoluteFile
        val parent = target.parentFile
            ?: throw IOException("Token file must have a parent directory.")
        if (!parent.exists() && !parent.mkdirs() && !parent.isDirectory) {
            throw IOException("Could not create the token directory.")
        }

        val temporary = File.createTempFile("token.", ".tmp", parent)
        try {
            temporary.writeText(accessToken.trim(), Charsets.UTF_8)
            try {
                Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        } finally {
            temporary.delete()
        }
    }

    @Synchronized
    override fun clearAccessToken() {
        if (file.exists() && !file.delete()) {
            throw IOException("Could not clear the stored access token.")
        }
    }
}
