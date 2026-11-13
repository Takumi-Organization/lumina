package com.adrian.lumina.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.adrian.lumina.domain.model.AlbumSort
import com.adrian.lumina.domain.model.AppSettings
import com.adrian.lumina.domain.model.ThemeMode
import com.adrian.lumina.domain.model.ThumbQuality
import com.adrian.lumina.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettings(private val store: DataStore<Preferences>) : SettingsRepository {
    private object K {
        val theme = stringPreferencesKey("theme")
        val columns = intPreferencesKey("default_columns")
        val quality = stringPreferencesKey("thumb_quality")
        val autoplay = booleanPreferencesKey("autoplay_videos")
        val animations = booleanPreferencesKey("animations")
        val haptics = booleanPreferencesKey("haptics")
        val sort = stringPreferencesKey("album_sort")
        val privacy = booleanPreferencesKey("privacy_screen")
    }

    private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    override val settings: Flow<AppSettings> = store.data.map { p ->
        val d = AppSettings()
        AppSettings(
            theme = p.enum(K.theme, d.theme),
            defaultColumns = p[K.columns] ?: d.defaultColumns,
            thumbQuality = p.enum(K.quality, d.thumbQuality),
            autoplayVideos = p[K.autoplay] ?: d.autoplayVideos,
            animations = p[K.animations] ?: d.animations,
            haptics = p[K.haptics] ?: d.haptics,
            albumSort = p.enum(K.sort, d.albumSort),
            privacyScreen = p[K.privacy] ?: d.privacyScreen,
        ).sanitized()
    }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { p ->
            val cur = AppSettings(
                p.enum(K.theme, ThemeMode.SYSTEM), p[K.columns] ?: 4, p.enum(K.quality, ThumbQuality.NORMAL),
                p[K.autoplay] ?: false, p[K.animations] ?: true, p[K.haptics] ?: true,
                p.enum(K.sort, AlbumSort.MANUAL), p[K.privacy] ?: false,
            )
            val n = transform(cur).sanitized()
            p[K.theme] = n.theme.name; p[K.columns] = n.defaultColumns; p[K.quality] = n.thumbQuality.name
            p[K.autoplay] = n.autoplayVideos; p[K.animations] = n.animations; p[K.haptics] = n.haptics
            p[K.sort] = n.albumSort.name; p[K.privacy] = n.privacyScreen
        }
    }
}
