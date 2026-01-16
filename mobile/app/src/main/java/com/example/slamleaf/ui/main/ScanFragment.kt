package com.example.slamleaf.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.slamleaf.data.local.PhotoRepository
import com.example.slamleaf.data.local.TokenManager
import com.example.slamleaf.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ScanFragment : Fragment() {

    private lateinit var cardMakePhoto: LinearLayout
    private lateinit var cardUploadGallery: LinearLayout
    private lateinit var tokenManager: TokenManager

    // picker z galerii
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleGalleryImage(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())

        cardMakePhoto = view.findViewById(R.id.cardMakePhoto)
        cardUploadGallery = view.findViewById(R.id.cardUploadGallery)

        // karta robienie zdjęcia
        cardMakePhoto.setOnClickListener {
            startActivity(Intent(requireContext(), CameraActivity::class.java))
        }
        // karta wyboru zdjęcia z galerii
        cardUploadGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun handleGalleryImage(uri: Uri) {
        // na podstawie Uri kopia do zdjecia z galerii do cache (dla Retrofit)
        viewLifecycleOwner.lifecycleScope.launch {
            val context = requireContext().applicationContext
            val tempFile = withContext(Dispatchers.IO) {
                val input = context.contentResolver.openInputStream(uri)
                val file = File(context.cacheDir, "gallery_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    input?.copyTo(out)
                }
                file
            }

            val localId = System.currentTimeMillis()
            PhotoRepository.addPending( tempFile.absolutePath)
            uploadPhotoFromGallery(tempFile, localId)
        }
    }

    private fun uploadPhotoFromGallery(file: File, localId: Long) {
        val token = tokenManager.getToken()

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Brak tokena – zaloguj się ponownie", Toast.LENGTH_SHORT).show()
            PhotoRepository.markError(localId)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val requestBody = file.asRequestBody("image/*".toMediaType())

                val multipart = MultipartBody.Part.createFormData(
                    name = "photo",
                    filename = file.name,
                    body = requestBody
                )

                val response = RetrofitClient.apiService.uploadPhoto(
                    photo = multipart,
                    token = "Bearer $token"
                )

                val fullUrl = RetrofitClient.BASE_URL.trimEnd('/') + response.url.trimStart('/')
                PhotoRepository.markSuccess(
                    localId = localId,
                    processedUrl = fullUrl,
                    detections = response.detections
                )
            } catch (e: Exception) {
                e.printStackTrace()
                PhotoRepository.markError(localId)
                Toast.makeText(requireContext(), "Błąd wysyłania zdjęcia", Toast.LENGTH_SHORT).show()
            }
        }
    }
}