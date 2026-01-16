package com.example.slamleaf.data.local

import com.example.slamleaf.DiseaseType
import com.example.slamleaf.PredictionStatus
import com.example.slamleaf.UiPhotoItem
import com.example.slamleaf.data.remote.DetectionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object PhotoRepository {

    private val _photos = MutableStateFlow<List<UiPhotoItem>>(emptyList())
    val photos: StateFlow<List<UiPhotoItem>> = _photos.asStateFlow()

    fun addPending(localPath: String): Long {
        val id = System.currentTimeMillis()
        val item = UiPhotoItem(
            localId = id,
            localPath = localPath,
            processedUrl = null,
            status = PredictionStatus.PENDING,
            mainDisease = null,
            confidence = null,
            detections = null
        )
        _photos.value = _photos.value + item
        return id
    }

    fun markSuccess(
        localId: Long,
        processedUrl: String,
        detections: List<DetectionDto>?
    ) {
        val safeDetections = detections ?: emptyList()
        val best = safeDetections.maxByOrNull { it.confidence }
        val mainDisease = best?.let { DiseaseType.Companion.fromClassId(it.class_id) }
        val conf = best?.confidence?.toFloat()

        _photos.value = _photos.value.map { item ->
            if (item.localId == localId) {
                item.copy(
                    processedUrl = processedUrl,
                    status = PredictionStatus.SUCCESS,
                    mainDisease = mainDisease,
                    confidence = conf,
                    detections = safeDetections
                )
            } else item
        }
    }

    fun markError(localId: Long) {
        _photos.value = _photos.value.map { item ->
            if (item.localId == localId) {
                item.copy(status = PredictionStatus.ERROR)
            } else item
        }
    }
    fun syncFromBackend(remoteItems: List<UiPhotoItem>) {
        _photos.update { current ->
            val merged = (remoteItems + current)
                .distinctBy { it.localId }          // bez duplikatów
                .sortedByDescending { it.localId }  // od najnowszych
            merged
        }
    }
    fun clear() {
        _photos.value = emptyList()
    }

}