
package com.pbl.grandmarket_android.network

import retrofit2.http.Body
import retrofit2.http.POST

// g: 카카오 로그인 요청용 데이터 모델
data class KakaoLoginRequest(val accessToken: String)

// g: 카카오 로그인 응답 데이터 모델(예시)
data class KakaoLoginResponse(val token: String, val userId: Long)

// g: 카카오 로그인 API 인터페이스
interface ApiService {
    @POST("/api/members/login/kakao")
    suspend fun kakaoLogin(@Body request: KakaoLoginRequest): retrofit2.Response<KakaoLoginResponse>
}
