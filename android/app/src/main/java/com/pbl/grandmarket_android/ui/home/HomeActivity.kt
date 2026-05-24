package com.pbl.grandmarket_android.ui.home

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.pbl.grandmarket_android.ui.map.MapSellerFragment
import com.pbl.grandmarket_android.ui.myinfo.MyInfoFragment
import com.pbl.grandmarket_android.R
import com.pbl.grandmarket_android.ui.item_list.SellListFragment
import com.pbl.grandmarket_android.data.model.UserRole
import com.pbl.grandmarket_android.data.local.UserSession
import com.pbl.grandmarket_android.databinding.ActivityHomeBinding
import com.pbl.grandmarket_android.ui.base.BaseActivity
import com.pbl.grandmarket_android.ui.map.MapBuyerFragment

class HomeActivity : BaseActivity() {
    private var backClickWait: Long = 0L

    private val homeBinding: ActivityHomeBinding by lazy {
        ActivityHomeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(homeBinding.root)
        applyBottomInsets(homeBinding.bottomMenuBar)
        val roleFromIntent = intent.getStringExtra(EXTRA_USER_ROLE)
        if (roleFromIntent != null) {
            UserSession.saveRole(this, UserRole.Companion.from(roleFromIntent))
        }

        // 초기 화면 설정
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        initBottomNavigation()

        onBackPressedDispatcher.addCallback(this, onBackPressCallback)
    }

    // bottom 버튼 누를 때 동작하는 기능
    private fun initBottomNavigation() {
        homeBinding.btnHome.setOnClickListener {
            replaceFragment(HomeFragment())
            updateBottomMenuUI(0)
        }
        homeBinding.btnMySellInfo.setOnClickListener {
            replaceFragment(SellListFragment())
            updateBottomMenuUI(1)
        }
        homeBinding.btnMap.setOnClickListener {
            val currentRole = UserSession.getRole(this) // UserSession에 구현된 get 메서드 사용

            if (currentRole == UserRole.BUYER) {
                replaceFragment(MapBuyerFragment()) // 새로 만들 구매자용 지도 뷰
            } else {
                replaceFragment(MapSellerFragment()) // 기존에 만든 판매자용 지도 뷰
            }
            updateBottomMenuUI(2)
        }
        homeBinding.btnMyInfo.setOnClickListener {
            replaceFragment(MyInfoFragment())
            updateBottomMenuUI(3)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, fragment::class.java.simpleName)
            .commit()
    }

    private fun updateBottomMenuUI(index: Int) {
        // 모든 메뉴 아이콘과 텍스트 색상을 기본으로 변경 (나중에 selector xml처리 예정)
        val defaultColor = ContextCompat.getColor(this, R.color.text_mid)
        val activeColor = ContextCompat.getColor(this, R.color.orange_main)

        homeBinding.textHome.setTextColor(if (index == 0) activeColor else defaultColor)
        homeBinding.textMySellInfo.setTextColor(if (index == 1) activeColor else defaultColor)
        homeBinding.textMap.setTextColor(if (index == 2) activeColor else defaultColor)
        homeBinding.textMyInfo.setTextColor(if (index == 3) activeColor else defaultColor)

        // 텍스트 스타일 변경 (Bold 여부)
        homeBinding.textHome.setTypeface(
            null, if (index == 0)
                Typeface.BOLD else Typeface.NORMAL
        )
        homeBinding.textMySellInfo.setTypeface(
            null, if (index == 1)
                Typeface.BOLD else Typeface.NORMAL
        )
        homeBinding.textMap.setTypeface(
            null, if (index == 2)
                Typeface.BOLD else Typeface.NORMAL
        )
        homeBinding.textMyInfo.setTypeface(
            null, if (index == 3)
                Typeface.BOLD else Typeface.NORMAL
        )
    }

    private val onBackPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (System.currentTimeMillis() - backClickWait >= 1500) {
                backClickWait = System.currentTimeMillis()
                Toast.makeText(application, "뒤로가기 버튼을 한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
                Log.d("클릭 이벤트", "뒤로가기 버튼 클릭됨")
            } else {
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_USER_ROLE = "extra_user_role"
    }
}
