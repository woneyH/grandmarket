package com.pbl.grandmarket_android.data.repository

import com.pbl.grandmarket_android.data.model.UserRole
import com.pbl.grandmarket_android.data.remote.ApiService
import com.pbl.grandmarket_android.data.remote.KakaoLoginRequest
import retrofit2.Response

class AuthRepository(private val apiService: ApiService) {
    suspend fun kakaoLogin(accessToken: String, role: UserRole): Response<String> {
        val request = KakaoLoginRequest(
            accessToken = accessToken,
            role = role.value
        )
        return apiService.kakaoLogin(request)
    }
}
