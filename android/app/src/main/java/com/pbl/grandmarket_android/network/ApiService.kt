
package com.pbl.grandmarket_android.network

import retrofit2.http.Body
import retrofit2.http.POST

// 카카오 로그인 요청용 데이터 모델
data class KakaoLoginRequest(
    val accessToken: String,
    val role: String
)

// 카카오 로그인 응답 데이터 모델
data class KakaoLoginResponse(val token: String, val userId: Long)

// 카카오 로그인 API 인터페이스
interface ApiService {
    @POST("/api/members/login/kakao")
    suspend fun kakaoLogin(@Body request: KakaoLoginRequest): retrofit2.Response<KakaoLoginResponse>
}
