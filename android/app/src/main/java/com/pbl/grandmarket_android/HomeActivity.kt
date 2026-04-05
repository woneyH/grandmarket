package com.pbl.grandmarket_android

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.pbl.grandmarket_android.databinding.ActivityHomeBinding


/**
 * 홈 액티비티 로그인 후 액티비티 전환용
 */
class HomeActivity : BaseActivity() {
    private var backClickWait: Long = 0L

    private val homeBinding: ActivityHomeBinding by lazy {
        ActivityHomeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(homeBinding.root)
        applyBottomInsets(homeBinding.bottomMenuBar)

        onBackPressedDispatcher.addCallback(this,onBackPressCallback)
    }

    private val onBackPressCallback = object: OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if(System.currentTimeMillis() - backClickWait >= 1500) {
                backClickWait = System.currentTimeMillis()
                Toast.makeText(application, "뒤로가기 버튼을 한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
                Log.d("클릭 이벤트","뒤로가기 버튼 클릭됨")
            }else {
                finish()
            }
        }
    }

}