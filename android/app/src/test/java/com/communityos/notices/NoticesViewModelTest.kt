package com.communityos.notices

import com.communityos.notices.domain.GetCommunityNoticesUseCase
import com.communityos.notices.event.NoticesListEvent
import com.communityos.notices.model.Notice
import com.communityos.notices.repository.NoticeRepository
import com.communityos.notices.viewmodel.NoticesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoticesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeNoticeRepository(
        var noticesResult: Result<List<Notice>> = Result.success(emptyList()),
        var detailResult: Result<Notice> = Result.failure(NoSuchElementException())
    ) : NoticeRepository {
        override suspend fun getCommunityNotices(): Result<List<Notice>> = noticesResult
        override suspend fun getNoticeDetails(noticeId: String): Result<Notice> = detailResult
    }

    private lateinit var fakeRepository: FakeNoticeRepository
    private lateinit var getCommunityNoticesUseCase: GetCommunityNoticesUseCase

    private val sampleNotices = listOf(
        Notice(
            id = "n1",
            communityId = "comm_1",
            title = "Water Maintenance",
            content = "Shutdown tomorrow",
            createdAt = 2000L
        ),
        Notice(
            id = "n2",
            communityId = "comm_1",
            title = "AGM Notice",
            content = "Sunday 5 PM",
            createdAt = 1000L
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeNoticeRepository(
            noticesResult = Result.success(sampleNotices)
        )
        getCommunityNoticesUseCase = GetCommunityNoticesUseCase(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialization_loadsNotices_successState() = runTest(testDispatcher) {
        val viewModel = NoticesViewModel(getCommunityNoticesUseCase)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(2, state.notices.size)
        assertEquals("n1", state.notices[0].id)
        assertNull(state.errorMessage)
    }

    @Test
    fun initialization_emptyNotices_successEmptyState() = runTest(testDispatcher) {
        fakeRepository.noticesResult = Result.success(emptyList())
        val viewModel = NoticesViewModel(getCommunityNoticesUseCase)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.notices.isEmpty())
        assertNull(state.errorMessage)
    }

    @Test
    fun initialization_failure_errorState() = runTest(testDispatcher) {
        fakeRepository.noticesResult = Result.failure(IllegalStateException("No active session found"))
        val viewModel = NoticesViewModel(getCommunityNoticesUseCase)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.notices.isEmpty())
        assertEquals("No active session found", state.errorMessage)
    }

    @Test
    fun refresh_reloadsNotices() = runTest(testDispatcher) {
        fakeRepository.noticesResult = Result.success(emptyList())
        val viewModel = NoticesViewModel(getCommunityNoticesUseCase)
        advanceUntilIdle()
        assertEquals(0, viewModel.state.value.notices.size)

        fakeRepository.noticesResult = Result.success(sampleNotices)
        viewModel.onEvent(NoticesListEvent.Refresh)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(2, state.notices.size)
    }

    @Test
    fun dismissError_clearsErrorMessage() = runTest(testDispatcher) {
        fakeRepository.noticesResult = Result.failure(RuntimeException("Network error"))
        val viewModel = NoticesViewModel(getCommunityNoticesUseCase)
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.errorMessage)

        viewModel.onEvent(NoticesListEvent.DismissError)
        assertNull(viewModel.state.value.errorMessage)
    }
}
