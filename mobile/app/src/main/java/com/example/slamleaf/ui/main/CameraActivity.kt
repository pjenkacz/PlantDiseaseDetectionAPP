package com.example.slamleaf.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.slamleaf.data.local.PhotoRepository
import com.example.slamleaf.data.local.TokenManager
import com.example.slamleaf.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class  CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var tokenManager: TokenManager
    private var imageCapture: ImageCapture? = null

    private val CAMERA_PERMISSION = Manifest.permission.CAMERA
    private val REQUEST_CAMERA = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        tokenManager = TokenManager(this)
        previewView = findViewById(R.id.previewView)
        btnCapture = findViewById(R.id.btnCapture)

        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(CAMERA_PERMISSION),
                REQUEST_CAMERA
            )
        }

        btnCapture.setOnClickListener {
            takePhotoAndUpload()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            Toast.makeText(this, "Brak zgody na użycie kamery", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Błąd kamery", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoAndUpload() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            "photo_${System.currentTimeMillis()}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    val localId = PhotoRepository.addPending(photoFile.absolutePath)
                    uploadPhoto(photoFile, localId)
                    val intent = Intent(this@CameraActivity, MainActivity::class.java).apply {
                        putExtra("openTab", "history")
                    }
                    startActivity(intent)
                }
                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    Toast.makeText(this@CameraActivity, "Nie udało się zrobić zdjęcia", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun uploadPhoto(file: File, localId: Long) {
        val token = tokenManager.getToken()

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Brak tokena – zaloguj się ponownie", Toast.LENGTH_SHORT).show()
            PhotoRepository.markError(localId)
            return
        }
        lifecycleScope.launch {
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
                Toast.makeText(
                    this@CameraActivity,
                    "Wysłano! id=${response.photoId}",
                    Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                PhotoRepository.markError(localId)
                Toast.makeText(
                    this@CameraActivity,
                    "Błąd wysyłania zdjęcia",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}