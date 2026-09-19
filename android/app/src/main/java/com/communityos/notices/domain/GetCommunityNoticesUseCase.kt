package com.communityos.notices.domain

import com.communityos.notices.model.Notice
import com.communityos.notices.repository.NoticeRepository
import javax.inject.Inject

class GetCommunityNoticesUseCase @Inject constructor(
    private val repository: NoticeRepository
) {
    suspend operator fun invoke(): Result<List<Notice>> {
        return repository.getCommunityNotices()
    }
}
