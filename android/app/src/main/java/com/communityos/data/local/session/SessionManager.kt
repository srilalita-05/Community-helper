package com.communityos.data.local.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.communityos.models.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

/**
 * Manages active user authentication session via Jetpack Preferences DataStore.
 * Safely handles missing/empty states by emitting null instead of throwing exceptions.
 */
@Singleton
class SessionManager(
    private val dataStore: DataStore<Preferences>
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.sessionDataStore)

    companion object {
        val KEY_USER_ID = stringPreferencesKey("session_user_id")
        val KEY_ROLE = stringPreferencesKey("session_role")
    }

    /**
     * Observes the active user session reactively.
     * Emits null if no session is active or if stored data is cleared.
     */
    fun observeSession(): Flow<Session?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val userId = preferences[KEY_USER_ID]
                val roleStr = preferences[KEY_ROLE]
                if (!userId.isNullOrBlank() && !roleStr.isNullOrBlank()) {
                    Session(
                        userId = userId,
                        role = UserRole.fromString(roleStr)
                    )
                } else {
                    null
                }
            }
    }

    /**
     * One-shot read of the active session. Returns null if not logged in.
     */
    suspend fun getSession(): Session? {
        return observeSession().firstOrNull()
    }

    /**
     * Persists the active session credentials.
     */
    suspend fun saveSession(userId: String, role: UserRole) {
        dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = userId
            preferences[KEY_ROLE] = role.name
        }
    }

    /**
     * Persists the active session credentials via [Session] model.
     */
    suspend fun saveSession(session: Session) {
        saveSession(session.userId, session.role)
    }

    /**
     * Clears the active session (e.g. on logout).
     */
    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
