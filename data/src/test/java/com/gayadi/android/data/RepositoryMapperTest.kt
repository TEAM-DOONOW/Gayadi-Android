package com.gayadi.android.data

import com.gayadi.android.data.datasource.InMemoryProfileLocalDataSource
import com.gayadi.android.data.datasource.FileProfileLocalDataSource
import com.gayadi.android.data.datasource.SurveyDataSource
import com.gayadi.android.data.model.SurveyDefinitionDto
import com.gayadi.android.data.model.SurveyOptionDto
import com.gayadi.android.data.model.SurveyQuestionDto
import com.gayadi.android.data.model.SurveyResultDto
import com.gayadi.android.data.repository.DefaultSurveyRepository
import com.gayadi.android.data.repository.InMemoryProfileRepository
import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyResult
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.gayadi.android.data.mapper.toDomain
import com.gayadi.android.data.model.LegalDocumentDto
import com.gayadi.android.data.model.LegalDocumentSectionDto
import kotlinx.coroutines.test.runTest

/** Verifies repository delegation and data-to-domain mapping. */
class RepositoryMapperTest {
    @Test
    fun legalDocumentMapper_preservesPublicationMetadataAndSections() {
        val document = LegalDocumentDto(
            id = "privacy-policy",
            title = "개인정보처리방침",
            version = "1.0.0",
            effectiveDate = "2026-08-12",
            summary = "개인정보 처리 안내",
            sections = listOf(LegalDocumentSectionDto("처리 목적", "여행 기능 제공")),
            reviewNotice = "검토 필요",
        ).toDomain()

        assertEquals("privacy-policy", document.id)
        assertEquals("처리 목적", document.sections.single().title)
        assertEquals("여행 기능 제공", document.sections.single().body)
        assertEquals("검토 필요", document.reviewNotice)
    }
    @Test
    fun profileRepository_mapsEntityAndDomainModel() = runTest {
        val repository = InMemoryProfileRepository(InMemoryProfileLocalDataSource())
        val expected = BasicInfo("가야디", "여행을 좋아해요")

        repository.saveBasicInfo(expected)

        assertEquals(expected, repository.getBasicInfo())
    }

    @Test
    fun profileRepository_persistsSurveyAcrossDataSourceRecreation() = runTest {
        val directory = Files.createTempDirectory("gayadi-profile-test").toFile()
        val profileFile = directory.resolve("profile.xml")
        val firstRepository = InMemoryProfileRepository(FileProfileLocalDataSource(profileFile))
        firstRepository.saveBasicInfo(BasicInfo("가야디", "여행가"))
        firstRepository.saveSurveyResult(
            SurveyResult(
                code = "SCA",
                emoji = "🔥",
                name = "즉흥 여행가",
                summary = "",
                hashtags = emptyList(),
                strengths = listOf("유연해요"),
                weaknesses = listOf("놓칠 수 있어요"),
                characterKey = "character_sca",
            ),
        )

        val recreatedRepository = InMemoryProfileRepository(FileProfileLocalDataSource(profileFile))

        assertEquals("가야디", recreatedRepository.getProfile()?.nickname)
        assertEquals("SCA", recreatedRepository.getProfile()?.resultCode)
        assertEquals("character_sca", recreatedRepository.getProfile()?.characterKey)
        assertEquals(listOf("유연해요"), recreatedRepository.getProfile()?.strengths)
        recreatedRepository.clearProfile().getOrThrow()
        assertNull(InMemoryProfileRepository(FileProfileLocalDataSource(profileFile)).getProfile())
        directory.deleteRecursively()
    }

    @Test
    fun profileRepository_rejectsSurveyWithoutBasicProfile() = runTest {
        val repository = InMemoryProfileRepository(InMemoryProfileLocalDataSource())

        val result = repository.saveSurveyResult(
            SurveyResult(
                code = "SCA",
                emoji = "🔥",
                name = "즉흥 여행가",
                summary = "",
                hashtags = emptyList(),
                strengths = emptyList(),
                weaknesses = emptyList(),
                characterKey = "character_sca",
            ),
        )

        assertEquals("기본 프로필이 없습니다.", result.exceptionOrNull()?.message)
        assertNull(repository.getProfile())
    }

    @Test
    fun surveyRepository_mapsAggregateToDomainModel() {
        val expectedResult = SurveyResultDto(
            "PNA",
            "⛰️",
            "플래너",
            "소개",
            listOf("#코스설계"),
            listOf("미리 조사해요."),
            listOf("조급해질 수 있어요."),
            "character_pna",
        )
        val dataSource = object : SurveyDataSource {
            override fun loadSurvey(callback: (Result<SurveyDefinitionDto>) -> Unit) {
                callback(
                    Result.success(
                        SurveyDefinitionDto(
                            id = "travel-personality-v1",
                            title = "여행 성향 판단 설문조사",
                            resultCodeOrder = listOf("preparation", "place", "energy"),
                            questions = listOf(
                                SurveyQuestionDto(
                                    id = "q01",
                                    order = 1,
                                    dimension = "preparation",
                                    title = "질문",
                                    options = listOf(SurveyOptionDto("a", "선택지", "P")),
                                ),
                            ),
                            results = listOf(expectedResult),
                        ),
                    ),
                )
            }

            override fun loadResult(code: String, callback: (Result<SurveyResultDto>) -> Unit) {
                callback(Result.success(expectedResult))
            }
        }
        val repository = DefaultSurveyRepository(dataSource)
        var questionTitle: String? = null
        var resultName: String? = null
        var resultCharacterKey: String? = null

        repository.loadSurvey { questionTitle = it.getOrThrow().questions.single().title }
        repository.loadResult("PNA") {
            resultName = it.getOrThrow().name
            resultCharacterKey = it.getOrThrow().characterKey
        }

        assertEquals("질문", questionTitle)
        assertEquals("플래너", resultName)
        assertEquals("character_pna", resultCharacterKey)
    }
}
