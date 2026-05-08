package com.pbl.grandmarket_android.repository

import com.pbl.grandmarket_android.UserRole
import com.pbl.grandmarket_android.network.ApiService
import com.pbl.grandmarket_android.network.KakaoLoginRequest
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
