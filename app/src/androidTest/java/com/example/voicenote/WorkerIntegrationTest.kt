package com.example.voicenote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.voicenote.core.workers.ReminderWorker
import com.example.voicenote.data.remote.ApiService
import com.example.voicenote.data.remote.TaskResponseDTO
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import javax.inject.Inject

@HiltAndroidTest
class WorkerIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var apiService: ApiService

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testReminderWorkerSuccess() = runBlocking {
        // Mock a task that is due soon to trigger escalation/notification logic
        val soon = System.currentTimeMillis() + 1000 * 60 * 10 // 10 minutes from now
        val fakeTasks = listOf(
            TaskResponseDTO(
                id = "task1",
                description = "Urgent Task",
                deadline = soon,
                priority = "MEDIUM",
                isDone = false,
                isDeleted = false
            )
        )

        coEvery { apiService.listTasks(any(), any(), any(), any()) } returns Response.success(fakeTasks)
        coEvery { apiService.updateTask("task1", any()) } returns Response.success(mockk(relaxed = true))

        val worker = TestListenableWorkerBuilder<ReminderWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }
}
