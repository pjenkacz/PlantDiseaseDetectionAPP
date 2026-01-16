package com.example.slamleaf.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.slamleaf.DiseaseType
import com.example.slamleaf.PhotoAdapter
import com.example.slamleaf.PhotoDetailActivity
import com.example.slamleaf.PredictionStatus
import com.example.slamleaf.UiPhotoItem
import com.example.slamleaf.data.local.PhotoRepository
import com.example.slamleaf.data.local.TokenManager
import com.example.slamleaf.data.remote.DetectionDto
import com.example.slamleaf.data.remote.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private lateinit var recyclerHistory: RecyclerView
    private lateinit var adapter: PhotoAdapter
    private lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())

        recyclerHistory = view.findViewById(R.id.recyclerPhotos)
        recyclerHistory.layoutManager = LinearLayoutManager(requireContext())

        adapter = PhotoAdapter { item ->
            openDetailScreen(item)
        }
        recyclerHistory.adapter = adapter

        // pobranie zdjęć użytkownika z backendu
        loadHistoryFromBackend()
        // korutyna aktualizująca listę zdjęć
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PhotoRepository.photos.collect { list ->
                    adapter.submitList(
                        list.sortedByDescending { it.localId }
                    )
                }
            }
        }
    }

    private fun loadHistoryFromBackend() {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Brak tokena – zaloguj się ponownie", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getPhotos("Bearer $token")
                // mapowanie odpowiedzi API -> UiPhotoItem
                val remoteItems = response.photos.map { photo ->
                    val fullUrl = RetrofitClient.BASE_URL.trimEnd('/') + photo.url
                    val mainDet = photo.detections.maxByOrNull { it.confidence }

                    UiPhotoItem(
                        localId = photo.id.toLong(),
                        localPath = "",                       // brak pliku lokalnego
                        processedUrl = fullUrl,               // URL z backendu
                        status = PredictionStatus.SUCCESS,    // z backendu zawsze SUCCESS
                        mainDisease = mainDet?.let { DiseaseType.Companion.fromClassId(it.class_id) },
                        confidence = mainDet?.confidence?.toFloat(),
                        detections = photo.detections
                    )
                }

                PhotoRepository.syncFromBackend(remoteItems)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Błąd ładowania historii", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openDetailScreen(item: UiPhotoItem) {
        val intent = Intent(requireContext(), PhotoDetailActivity::class.java).apply {
            putExtra("localPath", item.localPath)
            putExtra("processedUrl", item.processedUrl)
            putExtra("diseaseName", item.mainDisease?.displayName ?: "Brak rozpoznania")
            putExtra("confidence", item.confidence ?: -1f)
            putExtra("detectionsJson", Gson().toJson(item.detections ?: emptyList<DetectionDto>()))
        }
        startActivity(intent)
    }
}