package com.example.slamleaf.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface ApiService {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @PUT("user/changeName")
    suspend fun changeName(
        @Header("Authorization") token: String,
        @Body request: UserNameRequest
    ): Response<Unit>

    @GET("photos/get")
    suspend fun getPhotos(
        @Header("Authorization") token: String
    ): PhotoUploadListResponse

    @Multipart
    @POST("photos/upload")
    suspend fun uploadPhoto(
        @Part photo: MultipartBody.Part,
        @Header("Authorization") token: String
    ): PhotoUploadResponse
    // ===== DANE UŻYTKOWNIKA ======

    @PUT("user/changeEmail")
    suspend fun changeEmail(
        @Header("Authorization") token: String,
        @Body request: UserEmailRequest
    ): Response<Unit>


    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): RegisterResponse


    @PUT("user/changePassword")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: UserPasswordRequest
    ): Response<Unit>

    @GET("user/me")
    suspend fun getMe(
        @Header("Authorization") token: String
    ): UserMeResponse

}