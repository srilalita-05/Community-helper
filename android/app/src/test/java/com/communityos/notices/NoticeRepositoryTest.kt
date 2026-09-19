package com.communityos.notices

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.dao.NoticeDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.NoticeEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.session.SessionManager
import com.communityos.models.UserRole
import com.communityos.notices.repository.NoticeRepository
import com.communityos.notices.repository.NoticeRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class NoticeRepositoryTest {

    private lateinit var database: CommunityDatabase
    private lateinit var userDao: UserDao
    private lateinit var noticeDao: NoticeDao
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: NoticeRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
        noticeDao = database.noticeDao()

        val testFile = context.preferencesDataStoreFile("test_notice_repo_${System.nanoTime()}")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { testFile }
        )
        sessionManager = SessionManager(testDataStore)

        repository = NoticeRepositoryImpl(
            noticeDao = noticeDao,
            userDao = userDao,
            sessionManager = sessionManager
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun getCommunityNotices_returnsNoticesForActiveUserCommunity() = runTest {
        // Setup user with Orchard Heights community
        val user = UserEntity(
            id = "user_1",
            phoneNumber = "9876543210",
            name = "Resident One",
            communityId = "community_orchard",
            role = UserRole.RESIDENT
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_1", UserRole.RESIDENT)

        // Add notices for both Orchard and Palm
        val orchardNotice = NoticeEntity(
            id = "n_orchard",
            communityId = "community_orchard",
            title = "Orchard Notice",
            content = "Orchard Content",
            createdAt = 1000L
        )
        val palmNotice = NoticeEntity(
            id = "n_palm",
            communityId = "community_palm",
            title = "Palm Notice",
            content = "Palm Content",
            createdAt = 1100L
        )
        noticeDao.insertNotices(listOf(orchardNotice, palmNotice))

        val result = repository.getCommunityNotices()
        assertTrue(result.isSuccess)
        val notices = result.getOrNull()
        assertNotNull(notices)
        assertEquals(1, notices?.size)
        assertEquals("n_orchard", notices?.get(0)?.id)
        assertEquals("Orchard Notice", notices?.get(0)?.title)
    }

    @Test
    fun getCommunityNotices_failsSafelyWhenNoActiveSession() = runTest {
        val result = repository.getCommunityNotices()
        assertTrue(result.isFailure)
        assertEquals("No active session found", result.exceptionOrNull()?.message)
    }

    @Test
    fun getCommunityNotices_failsSafelyWhenUserNotInDatabase() = runTest {
        sessionManager.saveSession("non_existent_user", UserRole.RESIDENT)
        val result = repository.getCommunityNotices()
        assertTrue(result.isFailure)
        assertEquals("User not found for active session", result.exceptionOrNull()?.message)
    }

    @Test
    fun getCommunityNotices_failsSafelyWhenUserHasNoCommunity() = runTest {
        val user = UserEntity(
            id = "user_no_comm",
            phoneNumber = "1234567890",
            name = "Unassigned User",
            communityId = null
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_no_comm", UserRole.RESIDENT)

        val result = repository.getCommunityNotices()
        assertTrue(result.isFailure)
        assertEquals("User is not associated with any community", result.exceptionOrNull()?.message)
    }

    @Test
    fun getNoticeDetails_returnsNoticeWhenBelongingToActiveCommunity() = runTest {
        val user = UserEntity(
            id = "user_1",
            phoneNumber = "9876543210",
            name = "Resident One",
            communityId = "community_orchard",
            role = UserRole.RESIDENT
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_1", UserRole.RESIDENT)

        val notice = NoticeEntity(
            id = "notice_orchard_1",
            communityId = "community_orchard",
            title = "Lift Maintenance",
            content = "Lift B will be serviced on Friday.",
            createdAt = 5000L
        )
        noticeDao.insertNotice(notice)

        val result = repository.getNoticeDetails("notice_orchard_1")
        assertTrue(result.isSuccess)
        val details = result.getOrNull()
        assertNotNull(details)
        assertEquals("notice_orchard_1", details?.id)
        assertEquals("Lift Maintenance", details?.title)
        assertEquals("Lift B will be serviced on Friday.", details?.content)
    }

    @Test
    fun getNoticeDetails_failsWhenNoticeBelongsToDifferentCommunity() = runTest {
        // Active user is in Orchard
        val user = UserEntity(
            id = "user_1",
            phoneNumber = "9876543210",
            name = "Resident One",
            communityId = "community_orchard",
            role = UserRole.RESIDENT
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_1", UserRole.RESIDENT)

        // Notice belongs to Palm
        val palmNotice = NoticeEntity(
            id = "notice_palm_99",
            communityId = "community_palm",
            title = "Palm Security Alert",
            content = "Palm gate locked tonight.",
            createdAt = 5000L
        )
        noticeDao.insertNotice(palmNotice)

        // Requesting Palm notice while logged into Orchard
        val result = repository.getNoticeDetails("notice_palm_99")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun getNoticeDetails_failsWhenNoticeDoesNotExist() = runTest {
        val user = UserEntity(
            id = "user_1",
            phoneNumber = "9876543210",
            name = "Resident One",
            communityId = "community_orchard",
            role = UserRole.RESIDENT
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_1", UserRole.RESIDENT)

        val result = repository.getNoticeDetails("non_existent_notice")
        assertTrue(result.isFailure)
    }
}
