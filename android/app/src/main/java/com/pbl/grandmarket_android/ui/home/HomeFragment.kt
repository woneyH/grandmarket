package com.pbl.grandmarket_android.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kakao.sdk.user.UserApiClient
import com.pbl.grandmarket_android.R
import com.pbl.grandmarket_android.data.model.UserRole
import com.pbl.grandmarket_android.data.local.UserSession
import com.pbl.grandmarket_android.ui.item_list.RegisterItemBottomSheet

class HomeFragment : Fragment() {

    private var introMessageText: TextView? = null
    private var btnSearchRegister: View? = null
    private var homeSearchBar: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val role = UserSession.getRole(requireContext())

        val layoutRes = if (role == UserRole.SELLER) {
            R.layout.fragment_home
        } else {
            R.layout.fragment_home_buyer
        }

        val view = inflater.inflate(layoutRes, container, false)

        introMessageText = view.findViewById(R.id.intro_message_text)

        // 판매자일 때만 버튼 초기화
        if (role == UserRole.SELLER) {
            btnSearchRegister = view.findViewById(R.id.btnSearchRegister)
            homeSearchBar = view.findViewById(R.id.home_search_bar)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchKakaoProfile()

        // 버튼 클릭 시 바텀시트 띄우기
        btnSearchRegister?.setOnClickListener {
            showRegisterBottomSheet()
        }

        homeSearchBar?.setOnClickListener {
            showRegisterBottomSheet()
        }
    }

    private fun showRegisterBottomSheet() {
        // 바텀시트 열기 (등록 로직은 바텀시트 내부에서 처리됨)
        val bottomSheet = RegisterItemBottomSheet()
        bottomSheet.show(childFragmentManager, "RegisterItemBottomSheet")
    }

    private fun fetchKakaoProfile() {
        UserApiClient.instance.me { user, error ->
            val currentTextView = introMessageText ?: return@me

            if (error != null) {
                Log.e("HomeFragment", "카카오 정보 조회 실패", error)
                return@me
            }

            val nickname = user?.kakaoAccount?.profile?.nickname ?: "사용자"
            // 기존 텍스트 뒤에 닉네임 붙이기
            currentTextView.text = "${currentTextView.text} $nickname 님 🖐️"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        introMessageText = null
        btnSearchRegister = null
        homeSearchBar = null
    }
}