package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VirtualCamRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val videoDao = db.videoDao()
    private val settingsDao = db.appSettingsDao()

    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()
    val settingsFlow: Flow<AppSettingsEntity> = settingsDao.getSettingsFlow().map {
        it ?: AppSettingsEntity()
    }

    suspend fun initDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        videoDao.deleteSampleVideos()
        val currentSettings = settingsDao.getSettings()
        if (currentSettings == null) {
            settingsDao.insertOrUpdate(AppSettingsEntity())
        }

    }

    suspend fun addVideo(title: String, uriString: String, durationMs: Long, resolution: String) =
        withContext(Dispatchers.IO) {
            val count = videoDao.getVideoCount()
            val entity = VideoEntity(
                title = title,
                uriString = uriString,
                durationMs = durationMs,
                resolution = resolution,
                isSample = false,
                orderIndex = count
            )
            videoDao.insertVideo(entity)
        }

    suspend fun removeVideo(id: Long) = withContext(Dispatchers.IO) {
        videoDao.deleteVideoById(id)
    }

    suspend fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) =
        withContext(Dispatchers.IO) {
            val current = settingsDao.getSettings() ?: AppSettingsEntity()
            val updated = transform(current)
            settingsDao.insertOrUpdate(updated)
        }

    suspend fun getSettings(): AppSettingsEntity = withContext(Dispatchers.IO) {
        settingsDao.getSettings() ?: AppSettingsEntity()
    }
}
