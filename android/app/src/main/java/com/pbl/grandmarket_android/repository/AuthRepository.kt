package com.pbl.grandmarket_android.repository

import com.pbl.grandmarket_android.network.ApiService
import com.pbl.grandmarket_android.network.KakaoLoginRequest
import com.pbl.grandmarket_android.network.KakaoLoginResponse
import retrofit2.Response
class AuthRepository(private val apiService: ApiService) {
    suspend fun kakaoLogin(accessToken: String): Response<KakaoLoginResponse> {
        val request = KakaoLoginRequest(accessToken)
        return apiService.kakaoLogin(request)
    }

}