package com.lms.ayan.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SessionUtil(private val context: Context) {
    private val Context.dataStore by preferencesDataStore(
        name = USER_PREFERENCES
    )

    suspend fun setSession(isLogin: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SESSION_USER_LOGIN] = isLogin
        }
    }

    suspend fun isLoggedIn(): Boolean = context.dataStore.data.map { preferences ->
        preferences[SESSION_USER_LOGIN] ?: false
    }.first()

    suspend fun setUserId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[SESSION_USER_ID] = id
        }
    }

    suspend fun getUserId(): String = context.dataStore.data.map { preferences ->
        preferences[SESSION_USER_ID] ?: ""
    }.first()

    suspend fun setUserEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[SESSION_USER_EMAIL] = email
        }
    }

    suspend fun getUserEmail(): String = context.dataStore.data.map { preferences ->
        preferences[SESSION_USER_EMAIL] ?: ""
    }.first()

    suspend fun setUserFullName(fullName: String) {
        context.dataStore.edit { preferences ->
            preferences[SESSION_USER_NAME] = fullName
        }
    }

    suspend fun getUserFullName(): String = context.dataStore.data.map { preferences ->
        preferences[SESSION_USER_NAME] ?: ""
    }.first()

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[SESSION_LANG] = lang
        }
    }

    suspend fun getLanguage(): String = context.dataStore.data.map { preferences ->
        preferences[SESSION_LANG] ?: "en"
    }.first()

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[SESSION_USER_LOGIN] = false
            preferences[SESSION_USER_ID] = ""
            preferences[SESSION_USER_EMAIL] = ""
            preferences[SESSION_USER_NAME] = ""
            preferences[SESSION_LANG] = "en"
        }
    }


    companion object {
        private const val USER_PREFERENCES = "a_lms_prefs"
        private val SESSION_USER_LOGIN = booleanPreferencesKey("session_user_login")
        private val SESSION_USER_ID = stringPreferencesKey("session_user_id")
        private val SESSION_USER_EMAIL = stringPreferencesKey("session_user_email")
        private val SESSION_USER_NAME = stringPreferencesKey("session_user_name")

        private val SESSION_LANG = stringPreferencesKey("user_lang")

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: SessionUtil? = null

        fun getInstance(context: Context): SessionUtil {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let {
                    return it
                }

                val instance = SessionUtil(context)
                INSTANCE = instance
                instance
            }
        }
    }
}