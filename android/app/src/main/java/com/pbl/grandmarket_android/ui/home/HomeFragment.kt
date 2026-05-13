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

class HomeFragment : Fragment() {

    private var introMessageText: TextView? = null

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

        // seller / buyer 레이아웃 모두 introMessageText ID 동일해야 함
        introMessageText = view.findViewById(R.id.intro_message_text)

        Log.d("HomeFragment", "role=$role, layout=$layoutRes")

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchKakaoProfile()
    }

    private fun fetchKakaoProfile() {
        UserApiClient.instance.me { user, error ->

            val currentTextView = introMessageText ?: run {
                Log.e("HomeFragment", "introMessageText 찾기 실패")
                return@me
            }

            if (error != null) {
                Log.e("HomeFragment", "카카오 사용자 정보 조회 실패", error)
                return@me
            }

            val account = user?.kakaoAccount

            Log.d(
                "HomeFragment",
                "me 성공 userId=${user?.id}, profileNeedsAgreement=${account?.profileNeedsAgreement}, emailNeedsAgreement=${account?.emailNeedsAgreement}"
            )

            // 추가 동의 필요 시
            if (account?.profileNeedsAgreement == true || account?.emailNeedsAgreement == true) {
                Log.d("HomeFragment", "추가 동의 필요 -> loginWithNewScopes 호출")
                requestKakaoProfileScopes()
                return@me
            }

            val profile = account?.profile
            val nickname = profile?.nickname ?: "사용자"

            Log.d("HomeFragment", "nickname=$nickname")

            currentTextView.text = currentTextView.text.toString() + nickname +"님 🖐️"
        }
    }

    private fun requestKakaoProfileScopes() {
        val scopes = listOf("profile_nickname", "profile_image", "account_email")

        UserApiClient.instance.loginWithNewScopes(requireActivity(), scopes) { _, error ->
            if (error != null) {
                Log.e("HomeFragment", "카카오 추가 동의 요청 실패", error)
                return@loginWithNewScopes
            }

            Log.d("HomeFragment", "카카오 추가 동의 성공 -> 프로필 재조회")
            fetchKakaoProfile()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        introMessageText = null
    }
}