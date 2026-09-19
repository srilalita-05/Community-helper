package com.communityos.notices.repository

import com.communityos.data.local.dao.NoticeDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.session.SessionManager
import com.communityos.notices.model.Notice
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoticeRepositoryImpl @Inject constructor(
    private val noticeDao: NoticeDao,
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) : NoticeRepository {

    override suspend fun getCommunityNotices(): Result<List<Notice>> {
        val session = sessionManager.getSession()
            ?: return Result.failure(IllegalStateException("No active session found"))

        val user = userDao.getUserById(session.userId)
            ?: return Result.failure(IllegalStateException("User not found for active session"))

        val communityId = user.communityId
            ?: return Result.failure(IllegalStateException("User is not associated with any community"))

        val entities = noticeDao.getNoticesForCommunity(communityId)
        return Result.success(entities.map { it.toDomain() })
    }

    override suspend fun getNoticeDetails(noticeId: String): Result<Notice> {
        val session = sessionManager.getSession()
            ?: return Result.failure(IllegalStateException("No active session found"))

        val user = userDao.getUserById(session.userId)
            ?: return Result.failure(IllegalStateException("User not found for active session"))

        val communityId = user.communityId
            ?: return Result.failure(IllegalStateException("User is not associated with any community"))

        val entity = noticeDao.getNoticeByIdAndCommunity(noticeId, communityId)
            ?: return Result.failure(NoSuchElementException("Notice not found for the active community"))

        return Result.success(entity.toDomain())
    }
}
