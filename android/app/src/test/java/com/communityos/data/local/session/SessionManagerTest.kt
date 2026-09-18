package com.communityos.data.local.session

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.communityos.models.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionManagerTest {

    private fun createSessionManager(context: Context): SessionManager {
        val testFile = context.preferencesDataStoreFile("test_session_${System.nanoTime()}")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { testFile }
        )
        return SessionManager(testDataStore)
    }

    @Test
    fun empty_session_returns_null_safely() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = createSessionManager(context)

        val session = sessionManager.getSession()
        assertNull(session)
    }

    @Test
    fun saveSession_persists_userId_and_role() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = createSessionManager(context)

        sessionManager.saveSession("user_123", UserRole.RESIDENT)

        val session = sessionManager.getSession()
        assertNotNull(session)
        assertEquals("user_123", session?.userId)
        assertEquals(UserRole.RESIDENT, session?.role)
    }

    @Test
    fun clearSession_removes_session() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = createSessionManager(context)

        sessionManager.saveSession("user_456", UserRole.SECURITY)
        sessionManager.clearSession()

        val session = sessionManager.getSession()
        assertNull(session)
    }
}
