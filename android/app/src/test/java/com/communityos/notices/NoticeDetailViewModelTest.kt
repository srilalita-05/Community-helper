package com.communityos.notices

import androidx.lifecycle.SavedStateHandle
import com.communityos.notices.domain.GetNoticeDetailsUseCase
import com.communityos.notices.event.NoticeDetailEvent
import com.communityos.notices.model.Notice
import com.communityos.notices.repository.NoticeRepository
import com.communityos.notices.viewmodel.NoticeDetailViewModel
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
class NoticeDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeNoticeRepository(
        var detailResult: Result<Notice> = Result.failure(NoSuchElementException())
    ) : NoticeRepository {
        override suspend fun getCommunityNotices(): Result<List<Notice>> = Result.success(emptyList())
        override suspend fun getNoticeDetails(noticeId: String): Result<Notice> = detailResult
    }

    private lateinit var fakeRepository: FakeNoticeRepository
    private lateinit var getNoticeDetailsUseCase: GetNoticeDetailsUseCase

    private val sampleNotice = Notice(
        id = "notice_orchard_1",
        communityId = "community_orchard",
        title = "Lift Maintenance",
        content = "Detailed content about lift service.",
        createdAt = 1726650000000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeNoticeRepository(
            detailResult = Result.success(sampleNotice)
        )
        getNoticeDetailsUseCase = GetNoticeDetailsUseCase(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initializationWithNoticeId_loadsNotice_successState() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("noticeId" to "notice_orchard_1"))
        val viewModel = NoticeDetailViewModel(getNoticeDetailsUseCase, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.notice)
        assertEquals("notice_orchard_1", state.notice?.id)
        assertEquals("Lift Maintenance", state.notice?.title)
        assertNull(state.errorMessage)
    }

    @Test
    fun initializationWithNoticeId_notFound_errorState() = runTest(testDispatcher) {
        fakeRepository.detailResult = Result.failure(NoSuchElementException("Notice not found for the active community"))
        val savedStateHandle = SavedStateHandle(mapOf("noticeId" to "unauthorized_notice_id"))
        val viewModel = NoticeDetailViewModel(getNoticeDetailsUseCase, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.notice)
        assertEquals("Notice not found for the active community", state.errorMessage)
    }

    @Test
    fun loadNoticeEvent_loadsSpecifiedNotice() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        val viewModel = NoticeDetailViewModel(getNoticeDetailsUseCase, savedStateHandle)
        advanceUntilIdle()
        assertNull(viewModel.state.value.notice)

        viewModel.onEvent(NoticeDetailEvent.LoadNotice("notice_orchard_1"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.notice)
        assertEquals("notice_orchard_1", state.notice?.id)
    }

    @Test
    fun dismissError_clearsErrorMessage() = runTest(testDispatcher) {
        fakeRepository.detailResult = Result.failure(RuntimeException("Error"))
        val savedStateHandle = SavedStateHandle(mapOf("noticeId" to "some_id"))
        val viewModel = NoticeDetailViewModel(getNoticeDetailsUseCase, savedStateHandle)
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.errorMessage)

        viewModel.onEvent(NoticeDetailEvent.DismissError)
        assertNull(viewModel.state.value.errorMessage)
    }
}
