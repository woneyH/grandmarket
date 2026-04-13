package com.pbl.grandmarket_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import com.pbl.grandmarket_android.databinding.ActivityLoginBinding
// g: 카카오 로그인 SDK import
import com.kakao.sdk.user.UserApiClient
// g: Retrofit 관련 import
import com.pbl.grandmarket_android.network.ApiService
import com.pbl.grandmarket_android.network.KakaoLoginRequest
import com.pbl.grandmarket_android.network.KakaoLoginResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


/**
 *  로그인 액티비티 앱 실행 시 바로 보여질 액티비티 화면
 */
class LoginActivity : BaseActivity() {
    private val loginBinding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    // g: Retrofit 인스턴스 생성 (BaseUrl은 추후 변경 가능)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.0.19:8080") // gpt: 서버 주소
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // g: ApiService 생성
    private val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(loginBinding.root)
        applyBottomInsets(loginBinding.root)

        val intent = Intent(this, HomeActivity::class.java)
        val kakaoLoginBtn = loginBinding.btnKakaoLogin

        loginClickEvent(kakaoLoginBtn, intent)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    // g: 카카오 로그인 버튼 클릭 시 동작
    private fun loginClickEvent(loginBtn: AppCompatButton, intent: Intent) {
        loginBtn.setOnClickListener {
            // g: 카카오톡 앱/계정으로 로그인 시도
            UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
                if (error != null) {
                    Log.e("kakao login", "카카오 로그인 실패: $error")
                } else if (token != null) {
                    Log.d("kako login", "카카오 로그인 성공, accessToken: ${token.accessToken}")
                    Toast.makeText(this, "카카오 로그인 성공", Toast.LENGTH_SHORT).show()
                    val request = KakaoLoginRequest(token.accessToken)
                    apiService.kakaoLogin(request).enqueue(object : Callback<KakaoLoginResponse> {
                        override fun onResponse(
                            call: Call<KakaoLoginResponse>,
                            response: Response<KakaoLoginResponse>
                        ) {
                            Log.d(
                                "server response",
                                "서버 응답 코드: ${response.code()}, body: ${response.body()}, errorBody: ${
                                    response.errorBody()?.string()
                                }"
                            )
                            if (response.isSuccessful) {
                                Log.d("activity 이동", "HomeActivity로 이동 시도")
                                Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT)
                                    .show()
                                startActivity(intent)
                                finish()
                            } else {
                                Log.e("server response", "서버 로그인 실패: ${response.code()}")
                                Toast.makeText(
                                    this@LoginActivity,
                                    "서버 로그인 실패: ${response.code()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onFailure(call: Call<KakaoLoginResponse>, t: Throwable) {
                            Log.e("server response", "서버 통신 오류: $t")
                            Toast.makeText(this@LoginActivity, "서버 통신 오류: $t", Toast.LENGTH_SHORT)
                                .show()
                        }
                    })
                }
            }
        }
    }
}