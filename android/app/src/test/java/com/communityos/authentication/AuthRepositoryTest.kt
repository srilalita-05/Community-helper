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
import com.communityos.data.local.session.Session
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

    @Test
    fun restoreSession_withNoSession_returnsNull() = runTest {
        // DataStore has no session
        assertNull(sessionManager.getSession())

        val result = authRepository.restoreSession()

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun restoreSession_withValidSession_returnsUserAndRestoresState() = runTest {
        val user = UserEntity(
            id = "user_valid_1",
            phoneNumber = "9876543210",
            name = "Valid Resident",
            role = UserRole.RESIDENT
        )
        userDao.insertUser(user)
        sessionManager.saveSession(Session(userId = "user_valid_1", role = UserRole.RESIDENT))

        val result = authRepository.restoreSession()

        assertTrue(result.isSuccess)
        val authUser = result.getOrNull()
        assertNotNull(authUser)
        assertEquals("user_valid_1", authUser?.uid)
        assertEquals("Valid Resident", authUser?.displayName)
        assertEquals(UserRole.RESIDENT, authUser?.role)
    }

    @Test
    fun restoreSession_withInvalidSession_clearsSessionAndReturnsNull() = runTest {
        // Session points to non-existent user in Room
        sessionManager.saveSession(Session(userId = "ghost_user_999", role = UserRole.RESIDENT))
        assertNull(userDao.getUserById("ghost_user_999"))

        val result = authRepository.restoreSession()

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())

        // Invalid session must be cleared from DataStore
        assertNull(sessionManager.getSession())
    }

    @Test
    fun restoreSession_preservesPersistedRole() = runTest {
        val adminUser = UserEntity(
            id = "user_admin_1",
            phoneNumber = "9988776655",
            name = "Admin User",
            role = UserRole.ADMIN
        )
        userDao.insertUser(adminUser)
        sessionManager.saveSession(Session(userId = "user_admin_1", role = UserRole.ADMIN))

        val result = authRepository.restoreSession()

        assertTrue(result.isSuccess)
        val authUser = result.getOrNull()
        assertNotNull(authUser)
        assertEquals(UserRole.ADMIN, authUser?.role)

        // Session in DataStore remains intact with ADMIN role
        val currentSession = sessionManager.getSession()
        assertNotNull(currentSession)
        assertEquals(UserRole.ADMIN, currentSession?.role)
    }

    @Test
    fun logout_clearsPersistedSessionInDataStore() = runTest {
        authRepository.sendOtp("9876543210")
        authRepository.verifyOtp("123456")
        assertNotNull(sessionManager.getSession())

        val logoutResult = authRepository.logout()
        assertTrue(logoutResult.isSuccess)

        assertNull(sessionManager.getSession())
    }

    @Test
    fun logout_clearsInMemoryAuthenticatedUserState() = runTest {
        authRepository.sendOtp("9876543210")
        authRepository.verifyOtp("123456")

        val logoutResult = authRepository.logout()
        assertTrue(logoutResult.isSuccess)

        // Attempting an action requiring in-memory currentUser / active session must fail
        val selectResult = authRepository.selectCommunity("Orchard Heights Apartments")
        assertTrue(selectResult.isFailure)
        assertTrue(selectResult.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun logout_doesNotDeleteRoomUserOrCommunityData() = runTest {
        val community = CommunityEntity(
            id = "comm_test_1",
            name = "Test Community",
            address = "123 Street",
            city = "City",
            totalBlocks = 2
        )
        communityDao.insertCommunity(community)

        val flat = FlatEntity(
            id = "flat_101",
            communityId = "comm_test_1",
            block = "Block A",
            flatNumber = "A-101",
            floor = 1
        )
        flatDao.insertFlat(flat)

        authRepository.sendOtp("9876543210")
        authRepository.verifyOtp("123456")

        val userBeforeLogout = userDao.getUserByPhoneNumber("9876543210")
        assertNotNull(userBeforeLogout)

        val logoutResult = authRepository.logout()
        assertTrue(logoutResult.isSuccess)

        // Room user must still exist and be completely unchanged
        val userAfterLogout = userDao.getUserByPhoneNumber("9876543210")
        assertNotNull(userAfterLogout)
        assertEquals(userBeforeLogout?.id, userAfterLogout?.id)
        assertEquals(userBeforeLogout?.phoneNumber, userAfterLogout?.phoneNumber)
        assertEquals(userBeforeLogout?.name, userAfterLogout?.name)

        // Room community and flat data must be completely preserved
        val persistedCommunity = communityDao.getCommunityById("comm_test_1")
        assertNotNull(persistedCommunity)
        val persistedFlat = flatDao.getFlatByNumber("comm_test_1", "A-101")
        assertNotNull(persistedFlat)
    }

    @Test
    fun logout_subsequentRestoreSessionReturnsNoAuthenticatedSession() = runTest {
        authRepository.sendOtp("9876543210")
        authRepository.verifyOtp("123456")

        val logoutResult = authRepository.logout()
        assertTrue(logoutResult.isSuccess)

        val restoreResult = authRepository.restoreSession()
        assertTrue(restoreResult.isSuccess)
        assertNull(restoreResult.getOrNull())
    }
}
