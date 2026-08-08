package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.UserProfileEntity
import java.io.File
import java.nio.file.StandardCopyOption
import java.util.Properties

/** File-backed profile storage that survives process and app recreation. */
class FileProfileLocalDataSource(
    private val profileFile: File,
) : ProfileLocalDataSource {
    override fun saveProfile(profile: UserProfileEntity) {
        profileFile.parentFile?.mkdirs()
        val properties = Properties().apply {
            setProperty(NICKNAME, profile.nickname)
            setProperty(INTRODUCTION, profile.introduction)
            profile.resultCode?.let { setProperty(RESULT_CODE, it) }
            profile.travelStyleName?.let { setProperty(TRAVEL_STYLE_NAME, it) }
            profile.characterKey?.let { setProperty(CHARACTER_KEY, it) }
            setProperty(STRENGTHS, profile.strengths.joinToString(LIST_SEPARATOR))
            setProperty(WEAKNESSES, profile.weaknesses.joinToString(LIST_SEPARATOR))
        }
        val temporaryFile = File(profileFile.parentFile, "${profileFile.name}.tmp")
        temporaryFile.outputStream().buffered().use { properties.storeToXML(it, null) }
        java.nio.file.Files.move(
            temporaryFile.toPath(),
            profileFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    override fun getProfile(): UserProfileEntity? {
        if (!profileFile.isFile) return null
        val properties = Properties().apply {
            profileFile.inputStream().buffered().use(::loadFromXML)
        }
        val nickname = properties.getProperty(NICKNAME)?.takeIf(String::isNotBlank) ?: return null
        return UserProfileEntity(
            nickname = nickname,
            introduction = properties.getProperty(INTRODUCTION).orEmpty(),
            resultCode = properties.getProperty(RESULT_CODE)?.takeIf(String::isNotBlank),
            travelStyleName = properties.getProperty(TRAVEL_STYLE_NAME)?.takeIf(String::isNotBlank),
            characterKey = properties.getProperty(CHARACTER_KEY)?.takeIf(String::isNotBlank),
            strengths = properties.readList(STRENGTHS),
            weaknesses = properties.readList(WEAKNESSES),
        )
    }

    override fun clearProfile() {
        if (profileFile.exists() && !profileFile.delete()) {
            error("프로필 파일을 삭제하지 못했습니다.")
        }
    }

    private fun Properties.readList(key: String): List<String> =
        getProperty(key).orEmpty().split(LIST_SEPARATOR).filter(String::isNotBlank)

    private companion object {
        const val NICKNAME = "nickname"
        const val INTRODUCTION = "introduction"
        const val RESULT_CODE = "resultCode"
        const val TRAVEL_STYLE_NAME = "travelStyleName"
        const val CHARACTER_KEY = "characterKey"
        const val STRENGTHS = "strengths"
        const val WEAKNESSES = "weaknesses"
        const val LIST_SEPARATOR = "\u001F"
    }
}
