package com.communityos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.dao.NoticeDao
import com.communityos.data.local.entity.NoticeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class NoticeDaoTest {

    private lateinit var database: CommunityDatabase
    private lateinit var noticeDao: NoticeDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        noticeDao = database.noticeDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveNotice_byCommunity() = runTest {
        val notice = NoticeEntity(
            id = "notice_1",
            communityId = "community_orchard",
            title = "Maintenance Work",
            content = "Water shutdown tomorrow from 10am to 1pm.",
            createdAt = 1000L
        )
        noticeDao.insertNotice(notice)

        val retrieved = noticeDao.getNoticesForCommunity("community_orchard")
        assertEquals(1, retrieved.size)
        assertEquals("notice_1", retrieved[0].id)
        assertEquals("Maintenance Work", retrieved[0].title)
        assertEquals("Water shutdown tomorrow from 10am to 1pm.", retrieved[0].content)
    }

    @Test
    fun noticesAreOrderedByCreatedAtDescending() = runTest {
        val noticeOld = NoticeEntity(
            id = "notice_old",
            communityId = "community_orchard",
            title = "Old Notice",
            content = "Content old",
            createdAt = 1000L
        )
        val noticeNew = NoticeEntity(
            id = "notice_new",
            communityId = "community_orchard",
            title = "New Notice",
            content = "Content new",
            createdAt = 2000L
        )
        noticeDao.insertNotices(listOf(noticeOld, noticeNew))

        val list = noticeDao.getNoticesForCommunity("community_orchard")
        assertEquals(2, list.size)
        assertEquals("notice_new", list[0].id)
        assertEquals("notice_old", list[1].id)
    }

    @Test
    fun communityFiltering_doesNotLeakNoticesFromOtherCommunities() = runTest {
        val orchardNotice = NoticeEntity(
            id = "notice_orchard",
            communityId = "community_orchard",
            title = "Orchard Notice",
            content = "Orchard content",
            createdAt = 1500L
        )
        val palmNotice = NoticeEntity(
            id = "notice_palm",
            communityId = "community_palm",
            title = "Palm Notice",
            content = "Palm content",
            createdAt = 1600L
        )
        noticeDao.insertNotices(listOf(orchardNotice, palmNotice))

        val orchardList = noticeDao.getNoticesForCommunity("community_orchard")
        assertEquals(1, orchardList.size)
        assertEquals("notice_orchard", orchardList[0].id)

        val palmList = noticeDao.getNoticesForCommunity("community_palm")
        assertEquals(1, palmList.size)
        assertEquals("notice_palm", palmList[0].id)
    }

    @Test
    fun getNoticeByIdAndCommunity_returnsNoticeWhenCommunityMatches() = runTest {
        val notice = NoticeEntity(
            id = "notice_123",
            communityId = "community_orchard",
            title = "Title",
            content = "Content",
            createdAt = 1000L
        )
        noticeDao.insertNotice(notice)

        val found = noticeDao.getNoticeByIdAndCommunity("notice_123", "community_orchard")
        assertNotNull(found)
        assertEquals("notice_123", found?.id)
        assertEquals("Title", found?.title)
    }

    @Test
    fun getNoticeByIdAndCommunity_returnsNullWhenCommunityDoesNotMatch() = runTest {
        val notice = NoticeEntity(
            id = "notice_123",
            communityId = "community_orchard",
            title = "Title",
            content = "Content",
            createdAt = 1000L
        )
        noticeDao.insertNotice(notice)

        // Attempt to look up notice_123 using community_palm
        val notFound = noticeDao.getNoticeByIdAndCommunity("notice_123", "community_palm")
        assertNull(notFound)
    }

    @Test
    fun observeNoticesForCommunity_emitsUpdatedList() = runTest {
        val notice1 = NoticeEntity(
            id = "notice_1",
            communityId = "community_orchard",
            title = "First",
            content = "Content 1",
            createdAt = 1000L
        )
        noticeDao.insertNotice(notice1)

        val flowResult = noticeDao.observeNoticesForCommunity("community_orchard").first()
        assertEquals(1, flowResult.size)
        assertEquals("First", flowResult[0].title)
    }
}
