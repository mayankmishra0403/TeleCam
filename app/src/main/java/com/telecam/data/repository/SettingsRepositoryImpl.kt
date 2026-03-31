package com.telecam.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.telecam.domain.model.AppSettings
import com.telecam.domain.model.CameraFacing
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Implementation of SettingsRepository using DataStore.
 * Handles persistent app settings storage.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object PreferenceKeys {
        val AUTO_UPLOAD_ENABLED = booleanPreferencesKey("auto_upload_enabled")
        val WIFI_ONLY_UPLOAD = booleanPreferencesKey("wifi_only_upload")
        val BOT_TOKEN = stringPreferencesKey("bot_token")
        val CHAT_ID = stringPreferencesKey("chat_id")
        val MAX_RETRIES = intPreferencesKey("max_retries")
        val CAMERA_FACING = stringPreferencesKey("camera_facing")
    }

    override fun getSettings(): Flow<AppSettings> {
        return context.dataStore.data.map { preferences ->
            AppSettings(
                autoUploadEnabled = preferences[PreferenceKeys.AUTO_UPLOAD_ENABLED] ?: true,
                wifiOnlyUpload = preferences[PreferenceKeys.WIFI_ONLY_UPLOAD] ?: false,
                botToken = preferences[PreferenceKeys.BOT_TOKEN] ?: "",
                chatId = preferences[PreferenceKeys.CHAT_ID] ?: "",
                maxRetries = preferences[PreferenceKeys.MAX_RETRIES] ?: 3,
                cameraFacing = CameraFacing.valueOf(
                    preferences[PreferenceKeys.CAMERA_FACING] ?: CameraFacing.BACK.name
                )
            )
        }
    }

    override suspend fun getSettingsOnce(): AppSettings {
        return getSettings().first()
    }

    override suspend fun setAutoUploadEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_UPLOAD_ENABLED] = enabled
        }
    }

    override suspend fun setWifiOnlyUpload(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.WIFI_ONLY_UPLOAD] = enabled
        }
    }

    override suspend fun setBotToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.BOT_TOKEN] = token
        }
    }

    override suspend fun setChatId(chatId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.CHAT_ID] = chatId
        }
    }

    override suspend fun setMaxRetries(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.MAX_RETRIES] = count
        }
    }

    override suspend fun setCameraFacing(facing: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.CAMERA_FACING] = facing
        }
    }
}
