package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.model.AuthUser
import com.gayadi.android.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login submits normalized email and exposes completion`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository, dispatcher)

        viewModel.updateEmail("  user@example.com ")
        viewModel.updatePassword("password")
        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("user@example.com" to "password", repository.loginRequest)
        assertFalse(viewModel.uiState.value.completion!!.isNewAccount)
        assertEquals("", viewModel.uiState.value.password)
    }

    @Test
    fun `signup rejects short password before repository call`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository, dispatcher)

        viewModel.selectMode(AuthMode.SIGN_UP)
        viewModel.updateEmail("user@example.com")
        viewModel.updatePassword("12345")
        viewModel.updateNickname("가야디")
        viewModel.submit()

        assertEquals(null, repository.signupRequest)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `signup sends nickname and marks new account`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository, dispatcher)

        viewModel.selectMode(AuthMode.SIGN_UP)
        viewModel.updateEmail("user@example.com")
        viewModel.updatePassword("123456")
        viewModel.updateNickname("가야디")
        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Triple("user@example.com", "123456", "가야디"), repository.signupRequest)
        assertEquals(true, viewModel.uiState.value.completion?.isNewAccount)
    }
}

private class FakeAuthRepository : AuthRepository {
    var loginRequest: Pair<String, String>? = null
    var signupRequest: Triple<String, String, String>? = null

    override suspend fun signup(email: String, password: String, nickname: String): Result<AuthSession> {
        signupRequest = Triple(email, password, nickname)
        return Result.success(session(nickname))
    }

    override suspend fun login(email: String, password: String): Result<AuthSession> {
        loginRequest = email to password
        return Result.success(session("가야디"))
    }

    override suspend fun current() = Result.success(session("가야디").user)
    override suspend fun logout() = Result.success(Unit)
    override suspend fun withdraw() = Result.success(Unit)

    private fun session(nickname: String) = AuthSession(
        accessToken = "token",
        tokenType = "Bearer",
        expiresInSeconds = 3_600,
        user = AuthUser(id = 1, email = "user@example.com", nickname = nickname),
    )
}
