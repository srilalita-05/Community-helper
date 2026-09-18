package com.communityos.profile

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.FlatEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.session.SessionManager
import com.communityos.models.UserRole
import com.communityos.profile.repository.ProfileRepository
import com.communityos.profile.repository.ProfileRepositoryImpl
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
class ProfileRepositoryTest {

    private lateinit var database: CommunityDatabase
    private lateinit var userDao: UserDao
    private lateinit var communityDao: CommunityDao
    private lateinit var flatDao: FlatDao
    private lateinit var sessionManager: SessionManager
    private lateinit var profileRepository: ProfileRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
        communityDao = database.communityDao()
        flatDao = database.flatDao()

        val testFile = context.preferencesDataStoreFile("test_profile_repo_${System.nanoTime()}")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { testFile }
        )
        sessionManager = SessionManager(testDataStore)

        profileRepository = ProfileRepositoryImpl(
            userDao = userDao,
            communityDao = communityDao,
            flatDao = flatDao,
            sessionManager = sessionManager
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun getProfile_withActiveSessionAndUser_resolvesCommunityAndFlat() = runTest {
        val community = CommunityEntity(
            id = "comm_orchard",
            name = "Orchard Heights Apartments",
            address = "42 Greenfield Boulevard",
            city = "Bengaluru",
            totalBlocks = 4
        )
        communityDao.insertCommunity(community)

        val flat = FlatEntity(
            id = "flat_b304",
            communityId = "comm_orchard",
            block = "Block B",
            flatNumber = "B-304",
            floor = 3
        )
        flatDao.insertFlat(flat)

        val user = UserEntity(
            id = "user_resident_1",
            phoneNumber = "9876543210",
            name = "David Miller",
            email = "david@example.com",
            role = UserRole.RESIDENT,
            communityId = "comm_orchard",
            flatId = "flat_b304"
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_resident_1", UserRole.RESIDENT)

        val result = profileRepository.getProfile()
        assertTrue(result.isSuccess)

        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals("user_resident_1", profile?.id)
        assertEquals("David Miller", profile?.name)
        assertEquals("9876543210", profile?.phoneNumber)
        assertEquals("david@example.com", profile?.email)
        assertEquals("Orchard Heights Apartments", profile?.communityName)
        assertEquals("B-304", profile?.flatNumber)
        assertEquals(UserRole.RESIDENT, profile?.role)
    }

    @Test
    fun getProfile_withoutSession_returnsFailureSafely() = runTest {
        val result = profileRepository.getProfile()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun getProfile_withMissingUser_returnsFailureSafely() = runTest {
        sessionManager.saveSession("non_existent_user", UserRole.RESIDENT)

        val result = profileRepository.getProfile()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun updateProfile_withValidNameAndEmail_updatesRoomAndReturnsProfile() = runTest {
        val user = UserEntity(
            id = "user_10",
            phoneNumber = "9112233445",
            name = "Old Name",
            email = "old@example.com",
            role = UserRole.RESIDENT,
            communityId = null,
            flatId = null
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_10", UserRole.RESIDENT)

        val result = profileRepository.updateProfile(
            name = "Updated Name",
            email = "updated@example.com"
        )
        assertTrue(result.isSuccess)

        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals("Updated Name", profile?.name)
        assertEquals("updated@example.com", profile?.email)
    }

    @Test
    fun updateProfile_withBlankName_returnsFailure() = runTest {
        val user = UserEntity(
            id = "user_11",
            phoneNumber = "9112233446",
            name = "Initial Name",
            email = "init@example.com",
            role = UserRole.RESIDENT
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_11", UserRole.RESIDENT)

        val result = profileRepository.updateProfile(
            name = "   ",
            email = "valid@example.com"
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun updateProfile_withInvalidEmail_returnsFailure() = runTest {
        val user = UserEntity(
            id = "user_12",
            phoneNumber = "9112233447",
            name = "Initial Name",
            email = "init@example.com",
            role = UserRole.RESIDENT
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_12", UserRole.RESIDENT)

        val result = profileRepository.updateProfile(
            name = "Valid Name",
            email = "not-a-valid-email"
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun updateProfile_preservesImmutableFields() = runTest {
        val user = UserEntity(
            id = "user_immutable",
            phoneNumber = "9887766554",
            name = "Name Before",
            email = "before@example.com",
            role = UserRole.RESIDENT,
            communityId = "comm_orchard",
            flatId = "flat_b304",
            isApproved = true
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_immutable", UserRole.RESIDENT)

        val updateResult = profileRepository.updateProfile(
            name = "Name After",
            email = "after@example.com"
        )
        assertTrue(updateResult.isSuccess)

        val loadedEntity = userDao.getUserById("user_immutable")
        assertNotNull(loadedEntity)
        assertEquals("user_immutable", loadedEntity?.id)
        assertEquals("9887766554", loadedEntity?.phoneNumber)
        assertEquals(UserRole.RESIDENT, loadedEntity?.role)
        assertEquals("comm_orchard", loadedEntity?.communityId)
        assertEquals("flat_b304", loadedEntity?.flatId)
        assertEquals(true, loadedEntity?.isApproved)
    }

    @Test
    fun updateProfile_persistsChangesAcrossReloads() = runTest {
        val user = UserEntity(
            id = "user_persisted",
            phoneNumber = "9988776655",
            name = "First Name",
            email = "first@example.com",
            role = UserRole.RESIDENT
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_persisted", UserRole.RESIDENT)

        profileRepository.updateProfile(
            name = "Second Name",
            email = "second@example.com"
        )

        // Query fresh from repository
        val reloadResult = profileRepository.getProfile()
        assertTrue(reloadResult.isSuccess)
        assertEquals("Second Name", reloadResult.getOrNull()?.name)
        assertEquals("second@example.com", reloadResult.getOrNull()?.email)
    }
}
