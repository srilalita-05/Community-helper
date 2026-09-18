package com.communityos.authentication

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.authentication.repository.AuthRepository
import com.communityos.authentication.repository.AuthRepositoryImpl
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.FlatEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.session.SessionManager
import com.communityos.models.UserRole
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
class AuthRepositoryTest {

    private lateinit var database: CommunityDatabase
    private lateinit var userDao: UserDao
    private lateinit var communityDao: CommunityDao
    private lateinit var flatDao: FlatDao
    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
        communityDao = database.communityDao()
        flatDao = database.flatDao()

        val testFile = context.preferencesDataStoreFile("test_auth_repo_${System.nanoTime()}")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { testFile }
        )
        sessionManager = SessionManager(testDataStore)

        authRepository = AuthRepositoryImpl(
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
    fun verifyOtp_withExistingUser_loadsUserAndSetsSessionWithoutDuplication() = runTest {
        val existing = UserEntity(
            id = "user_existing_1",
            phoneNumber = "9876543210",
            name = "Existing Resident",
            role = UserRole.RESIDENT
        )
        userDao.insertUser(existing)

        authRepository.sendOtp("9876543210")
        val result = authRepository.verifyOtp("123456")

        assertTrue(result.isSuccess)
        val authUser = result.getOrNull()
        assertNotNull(authUser)
        assertEquals("user_existing_1", authUser?.uid)
        assertFalse(authUser?.isNewUser ?: true)

        // Ensure user row is reused and not duplicated
        val loaded = userDao.getUserByPhoneNumber("9876543210")
        assertNotNull(loaded)
        assertEquals("Existing Resident", loaded?.name)

        // Ensure session is persisted
        val session = sessionManager.getSession()
        assertNotNull(session)
        assertEquals("user_existing_1", session?.userId)
        assertEquals(UserRole.RESIDENT, session?.role)
    }

    @Test
    fun verifyOtp_withNewUser_createsAndPersistsUserAndSetsSession() = runTest {
        authRepository.sendOtp("9112233445")
        val result = authRepository.verifyOtp("123456")

        assertTrue(result.isSuccess)
        val authUser = result.getOrNull()
        assertNotNull(authUser)

        val persisted = userDao.getUserByPhoneNumber("9112233445")
        assertNotNull(persisted)
        assertEquals(authUser?.uid, persisted?.id)
        assertEquals(UserRole.RESIDENT, persisted?.role)

        val session = sessionManager.getSession()
        assertNotNull(session)
        assertEquals(persisted?.id, session?.userId)
        assertEquals(UserRole.RESIDENT, session?.role)
    }

    @Test
    fun registerUser_updatesNameAndEmailInRoom() = runTest {
        authRepository.sendOtp("9888877777")
        authRepository.verifyOtp("123456")

        val regResult = authRepository.registerUser("Sarah Connor", "sarah@resistance.org")
        assertTrue(regResult.isSuccess)

        val user = userDao.getUserByPhoneNumber("9888877777")
        assertNotNull(user)
        assertEquals("Sarah Connor", user?.name)
        assertEquals("sarah@resistance.org", user?.email)
    }

    @Test
    fun selectCommunityAndVerifyFlat_updatesCommunityFlatAndRoleInRoomAndSession() = runTest {
        val community = CommunityEntity(
            id = "comm_orchard",
            name = "Orchard Heights Apartments",
            address = "123 Orchard Road",
            city = "Bengaluru",
            totalBlocks = 3
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

        authRepository.sendOtp("9900112233")
        authRepository.verifyOtp("123456")

        val commResult = authRepository.selectCommunity("Orchard Heights Apartments")
        assertTrue(commResult.isSuccess)

        val flatResult = authRepository.verifyFlat("B-304", "Security")
        assertTrue(flatResult.isSuccess)

        val updatedUser = userDao.getUserByPhoneNumber("9900112233")
        assertNotNull(updatedUser)
        assertEquals("comm_orchard", updatedUser?.communityId)
        assertEquals("flat_b304", updatedUser?.flatId)
        assertEquals(UserRole.SECURITY, updatedUser?.role)

        val session = sessionManager.getSession()
        assertEquals(UserRole.SECURITY, session?.role)
    }

    @Test
    fun verifyFlat_failsIfFlatDoesNotBelongToSelectedCommunity() = runTest {
        val community = CommunityEntity(
            id = "comm_orchard",
            name = "Orchard Heights Apartments",
            address = "123 Orchard Road",
            city = "Bengaluru",
            totalBlocks = 3
        )
        communityDao.insertCommunity(community)

        authRepository.sendOtp("9900112244")
        authRepository.verifyOtp("123456")
        authRepository.selectCommunity("Orchard Heights Apartments")

        // Flat does not exist in Orchard Heights
        val flatResult = authRepository.verifyFlat("Z-999", "Resident")
        assertTrue(flatResult.isFailure)

        val user = userDao.getUserByPhoneNumber("9900112244")
        assertNull(user?.flatId)
    }
}
