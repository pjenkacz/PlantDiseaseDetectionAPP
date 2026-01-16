package com.example.slamleaf.data.remote

// ===== REQUEST DATA CLASSES =====

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

// ===== RESPONSE DATA CLASSES =====

data class LoginResponse(
    val token: String,
    val userId: String,
    val message: String? = null
)

data class RegisterResponse(
    val token: String,
    val userId: String,
    val message: String? = null
)

data class DetectionDto(
    val class_id: Int,
    val confidence: Float,
    val box: List<Float>
)
data class PhotoUploadResponse(
    val photoId: String,
    val url: String,
    val message: String,
    val detections: List<DetectionDto>
)
data class PhotoHistoryResponse(
    val id: Int,
    val url: String,
    val detections: List<DetectionDto> = emptyList()
)
data class PhotoUploadListResponse(
    val photos: List<PhotoHistoryResponse> = emptyList()
)
data class ApiError(
    val error: String,
    val message: String
)
data class UserMeResponse(
    val id: Int,
    val email: String,
    val name: String
)
data class UserEmailRequest(
    val email: String,
    val password: String
)

data class UserNameRequest(
    val name: String,
    val password: String
)

data class UserPasswordRequest(
    val password: String,
    val newPassword: String
)

data class UserEmailResponse(
    val message: String
)
