package com.pbl.grandmarket_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.pbl.grandmarket_android.databinding.ActivityLoginBinding


/**
 *  로그인 액티비티 앱 실행 시 바로 보여질 액티비티 화면
 */
class LoginActivity : BaseActivity() {
    private val loginBinding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(loginBinding.root)
        applyBottomInsets(loginBinding.root)

        val intent = Intent(this, HomeActivity::class.java)
        val kakaoLoginBtn = loginBinding.btnKakaoLogin

        loginClickEvent(kakaoLoginBtn, intent)
    }

    private fun loginClickEvent(loginBtn: AppCompatButton, intent: Intent) {
        loginBtn.setOnClickListener {
            startActivity(intent)

            Log.d("클릭 이벤트","로그인 버튼 클릭됨")
        }
    }
}