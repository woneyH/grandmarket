package com.pbl.grandmarket_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import com.kakao.sdk.user.UserApiClient
import com.pbl.grandmarket_android.databinding.ActivityLoginBinding
import com.pbl.grandmarket_android.network.ApiService
import com.pbl.grandmarket_android.repository.AuthRepository
import com.pbl.grandmarket_android.util.Resource
import com.pbl.grandmarket_android.view_model.LoginViewModel
import com.pbl.grandmarket_android.view_model.LoginViewModelFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 *  로그인 액티비티 앱 실행 시 바로 보여질 액티비티 화면
 */
class LoginActivity : BaseActivity() {
    private val IS_SKIP_KAKAO_LOGIN = false
    private val serverIp = "http://192.168.0.19:8080"
    private val loginBinding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    // ViewModel 초기화 (변경된 패키지 경로 적용: view_model)
    private val viewModel: LoginViewModel by viewModels {
        val retrofit = Retrofit.Builder()
            .baseUrl(serverIp)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        LoginViewModelFactory(AuthRepository(apiService))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(loginBinding.root)
        applyBottomInsets(loginBinding.root)

        setupObservers()
        setupListeners(IS_SKIP_KAKAO_LOGIN)
    }

    private fun setupObservers() {
        viewModel.loginStatus.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    Log.d("LoginActivity", "로그인 진행 중...")
                }
                is Resource.Success -> {
                    Log.d("LoginActivity", "로그인 성공: ${resource.data}")
                    Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                    val role = viewModel.loginRole.value ?: UserRole.BUYER
                    UserSession.saveRole(this, role)
                    moveToHome(role)
                }
                is Resource.Error<*> -> {
                    val errorMessage = resource.data?.toString()?:"로그인 실패"
                    Log.e("LoginActivity", "서버 로그인 에러: $errorMessage")
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //홈 엑티비티와 UI 원할한 업데이트를 위해 카카오 로그인 생략 추가함.
    private fun setupListeners(skipKakaoLogin: Boolean) {
        val sellerBtn = loginBinding.btnSellerLogin
        val buyerBtn = loginBinding.btnBuyerLogin

        // 카카오 로그인 클릭 이벤트
        if(skipKakaoLogin) {
            sellerBtn.setOnClickListener {
                UserSession.saveRole(this, UserRole.SELLER)
                moveToHome(UserRole.SELLER)
            }
            buyerBtn.setOnClickListener  {
                UserSession.saveRole(this, UserRole.BUYER)
                moveToHome(UserRole.BUYER)
            }
        }else {
            sellerBtn.setOnClickListener {
                UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
                    if (error != null) {
                        Log.e("kakao login", "카카오 로그인 실패: $error")
                    } else if (token != null) {
                        Log.d("kakao login", "카카오 로그인 성공")
                        viewModel.performKakaoLogin(token.accessToken, UserRole.SELLER)
                    }
                }
            }

            buyerBtn.setOnClickListener {
                UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
                    if (error != null) {
                        Log.e("kakao login", "카카오 로그인 실패: $error")
                    } else if (token != null) {
                        Log.d("kakao login", "카카오 로그인 성공")
                        viewModel.performKakaoLogin(token.accessToken, UserRole.BUYER)
                    }
                }
            }
        }

    }

    private fun moveToHome(role: UserRole) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra(HomeActivity.EXTRA_USER_ROLE, role.value)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
