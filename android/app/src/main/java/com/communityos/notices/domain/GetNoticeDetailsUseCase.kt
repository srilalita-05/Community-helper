package com.communityos.notices.domain

import com.communityos.notices.model.Notice
import com.communityos.notices.repository.NoticeRepository
import javax.inject.Inject

class GetNoticeDetailsUseCase @Inject constructor(
    private val repository: NoticeRepository
) {
    suspend operator fun invoke(noticeId: String): Result<Notice> {
        return repository.getNoticeDetails(noticeId)
    }
}
