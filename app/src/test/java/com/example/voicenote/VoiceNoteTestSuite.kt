package com.example.voicenote

import com.example.voicenote.core.service.VoiceRecordingService
import com.example.voicenote.data.repository.VoiceNoteRepositoryImpl
import com.example.voicenote.data.remote.ApiService
import com.example.voicenote.core.security.SecurityManager
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Before
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.times
import org.mockito.kotlin.mock
import org.mockito.kotlin.doReturn
import java.io.File
import okhttp3.ResponseBody
import retrofit2.Response
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Comprehensive test suite for VoiceNote application
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class VoiceNoteTestSuite {

    @Mock
    private lateinit var mockApiService: ApiService

    @Mock
    private lateinit var mockSecurityManager: SecurityManager

    private lateinit var repository: VoiceNoteRepositoryImpl
    private lateinit var dispatcher: TestCoroutineDispatcher

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        dispatcher = TestCoroutineDispatcher()
        repository = VoiceNoteRepositoryImpl(mockApiService, mockSecurityManager, dispatcher)
    }

    /**
     * Test for successful voice note upload
     */
    @Test
    fun `test uploadVoiceNote success`() = runBlockingTest {
        // Given
        val mockFile = mock<File> {
            on { name } doReturn "test_audio.mp4"
            on { length() } doReturn 1024000L
        }
        val mockResponse = Response.success(mapOf("note_id" to "test_note_id"))
        
        whenever(mockApiService.uploadVoiceNote(any(), eq("test_audio.mp4"), any()))
            .thenReturn(mockResponse)

        // When
        val result = repository.uploadVoiceNote(mockFile, "transcribe")

        // Then
        verify(mockApiService, times(1)).uploadVoiceNote(any(), eq("test_audio.mp4"), any())
        assert(result.isSuccess)
    }

    /**
     * Test for failed voice note upload
     */
    @Test
    fun `test uploadVoiceNote failure`() = runBlockingTest {
        // Given
        val mockFile = mock<File> {
            on { name } doReturn "test_audio.mp4"
            on { length() } doReturn 1024000L
        }
        val mockResponse = Response.error<Map<String, String>>(500, mock<ResponseBody>())

        whenever(mockApiService.uploadVoiceNote(any(), eq("test_audio.mp4"), any()))
            .thenReturn(mockResponse)

        // When
        val result = repository.uploadVoiceNote(mockFile, "transcribe")

        // Then
        verify(mockApiService, times(1)).uploadVoiceNote(any(), eq("test_audio.mp4"), any())
        assert(result.isFailure)
    }

    /**
     * Test for retrieving all notes
     */
    @Test
    fun `test getNotes success`() = runBlockingTest {
        // Given
        val mockNotesResponse = mapOf(
            "notes" to listOf(
                mapOf(
                    "id" to "1",
                    "title" to "Test Note",
                    "summary" to "Test summary",
                    "createdAt" to "2023-01-01T00:00:00Z"
                )
            )
        )
        val mockResponse = Response.success(mockNotesResponse)

        whenever(mockApiService.getNotes()).thenReturn(mockResponse)

        // When
        val result = repository.getNotes()

        // Then
        verify(mockApiService, times(1)).getNotes()
        assert(result.isSuccess)
        assert(result.getOrNull()?.size == 1)
    }

    /**
     * Test for retrieving a specific note
     */
    @Test
    fun `test getNote success`() = runBlockingTest {
        // Given
        val noteId = "1"
        val mockNoteResponse = mapOf(
            "id" to noteId,
            "title" to "Test Note",
            "summary" to "Test summary",
            "transcript" to "Test transcript",
            "createdAt" to "2023-01-01T00:00:00Z"
        )
        val mockResponse = Response.success(mockNoteResponse)

        whenever(mockApiService.getNote(eq(noteId))).thenReturn(mockResponse)

        // When
        val result = repository.getNote(noteId)

        // Then
        verify(mockApiService, times(1)).getNote(eq(noteId))
        assert(result.isSuccess)
        assert(result.getOrNull()?.get("id") == noteId)
    }

    /**
     * Test for asking AI about a note
     */
    @Test
    fun `test askAI success`() = runBlockingTest {
        // Given
        val noteId = "1"
        val question = "What is this about?"
        val mockAiResponse = mapOf(
            "answer" to "This is about testing",
            "confidence" to 0.95
        )
        val mockResponse = Response.success(mockAiResponse)

        whenever(mockApiService.askAI(eq(noteId), any())).thenReturn(mockResponse)

        // When
        val result = repository.askAI(noteId, question)

        // Then
        verify(mockApiService, times(1)).askAI(eq(noteId), any())
        assert(result.isSuccess)
        assert(result.getOrNull()?.containsKey("answer") == true)
    }

    /**
     * Test for user login
     */
    @Test
    fun `test login success`() = runBlockingTest {
        // Given
        val credentials = mapOf("email" to "test@example.com", "password" to "password123")
        val mockLoginResponse = mapOf(
            "access_token" to "mock_access_token",
            "refresh_token" to "mock_refresh_token",
            "user_id" to "12345"
        )
        val mockResponse = Response.success(mockLoginResponse)

        whenever(mockApiService.login(any())).thenReturn(mockResponse)

        // When
        val result = repository.login("test@example.com", "password123")

        // Then
        verify(mockApiService, times(1)).login(any())
        assert(result.isSuccess)
        assert(result.getOrNull()?.containsKey("access_token") == true)
    }

    /**
     * Test for user registration
     */
    @Test
    fun `test register success`() = runBlockingTest {
        // Given
        val userData = mapOf(
            "email" to "newuser@example.com",
            "password" to "password123",
            "name" to "New User"
        )
        val mockRegisterResponse = mapOf(
            "id" to "67890",
            "email" to "newuser@example.com",
            "created_at" to "2023-01-01T00:00:00Z"
        )
        val mockResponse = Response.success(mockRegisterResponse)

        whenever(mockApiService.register(any())).thenReturn(mockResponse)

        // When
        val result = repository.register("newuser@example.com", "password123", "New User")

        // Then
        verify(mockApiService, times(1)).register(any())
        assert(result.isSuccess)
        assert(result.getOrNull()?.containsKey("id") == true)
    }

    /**
     * Test for getting user profile
     */
    @Test
    fun `test getUserProfile success`() = runBlockingTest {
        // Given
        val mockUserProfile = mapOf(
            "id" to "12345",
            "email" to "test@example.com",
            "name" to "Test User",
            "created_at" to "2023-01-01T00:00:00Z"
        )
        val mockResponse = Response.success(mockUserProfile)

        whenever(mockApiService.getUserProfile()).thenReturn(mockResponse)

        // When
        val result = repository.getUserProfile()

        // Then
        verify(mockApiService, times(1)).getUserProfile()
        assert(result.isSuccess)
        assert(result.getOrNull()?.containsKey("email") == true)
    }
}