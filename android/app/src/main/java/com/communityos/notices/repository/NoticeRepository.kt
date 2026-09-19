package com.communityos.notices.repository

import com.communityos.notices.model.Notice

interface NoticeRepository {
    suspend fun getCommunityNotices(): Result<List<Notice>>
    suspend fun getNoticeDetails(noticeId: String): Result<Notice>
}
