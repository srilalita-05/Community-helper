package com.communityos.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.entity.UserEntity
import com.communityos.models.UserRole
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class UserDaoTest {

    private lateinit var database: CommunityDatabase
    private lateinit var userDao: UserDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertUser_and_getUserById_returns_matching_entity() = runTest {
        val user = UserEntity(
            id = "user_1",
            phoneNumber = "+1234567890",
            name = "Alice Resident",
            email = "alice@example.com",
            role = UserRole.RESIDENT,
            communityId = "community_orchard",
            flatId = "flat_b304",
            isApproved = true
        )

        userDao.insertUser(user)
        val loaded = userDao.getUserById("user_1")

        assertNotNull(loaded)
        assertEquals("Alice Resident", loaded?.name)
        assertEquals("+1234567890", loaded?.phoneNumber)
        assertEquals(UserRole.RESIDENT, loaded?.role)
        assertTrue(loaded?.isApproved == true)
    }

    @Test
    fun getUserByPhoneNumber_returns_correct_user() = runTest {
        val user = UserEntity(
            id = "user_2",
            phoneNumber = "+9876543210",
            name = "Bob Security",
            role = UserRole.SECURITY
        )

        userDao.insertUser(user)
        val loaded = userDao.getUserByPhoneNumber("+9876543210")

        assertNotNull(loaded)
        assertEquals("user_2", loaded?.id)
        assertEquals(UserRole.SECURITY, loaded?.role)
    }

    @Test
    fun updateUserRole_updates_role_in_database() = runTest {
        val user = UserEntity(
            id = "user_3",
            phoneNumber = "+1122334455",
            name = "Charlie Admin",
            role = UserRole.RESIDENT
        )

        userDao.insertUser(user)
        userDao.updateUserRole("user_3", UserRole.ADMIN)

        val updated = userDao.getUserById("user_3")
        assertNotNull(updated)
        assertEquals(UserRole.ADMIN, updated?.role)
    }
}
