package com.pbl.grandmarket_android.ui.myinfo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import coil.load
import com.kakao.sdk.user.UserApiClient
import com.pbl.grandmarket_android.data.local.UserSession
import com.pbl.grandmarket_android.data.model.UserRole
import com.pbl.grandmarket_android.data.repository.FoodRepository
import com.pbl.grandmarket_android.databinding.FragmentMyInfoBinding
import com.pbl.grandmarket_android.ui.login.LoginActivity

class MyInfoFragment : Fragment() {
    private var _binding: FragmentMyInfoBinding? = null
    private val binding get() = _binding!!

    // 판매자 통계 로드를 위한 레포지토리
    private val foodRepository = FoodRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindLogoutAction()
        fetchKakaoProfile()
        // 판매자로 로그인한 경우에만 판매 통계(총 판매, 판매완료, 조회수) 로드
        if (UserSession.getRole(requireContext()) == UserRole.SELLER) {
            loadSellerStats()
        }
    }

    // Firebase에서 판매자 통계 조회 후 tvSaleCount, tvActiveCount, tvViewCount에 반영
    private fun loadSellerStats() {
        foodRepository.getSellerStats { totalCount, soldCount, viewCount ->
            val currentBinding = _binding ?: return@getSellerStats
            currentBinding.tvSaleCount.text = totalCount.toString()
            currentBinding.tvActiveCount.text = soldCount.toString()
            currentBinding.tvViewCount.text = viewCount.toString()
        }
    }

    private fun fetchKakaoProfile() {
        Log.d("MyInfoFragment", "fetchKakaoProfile() 호출")
        UserApiClient.Companion.instance.me { user, error ->
            val currentBinding = _binding ?: return@me

            if (error != null) {
                Log.e("MyInfoFragment", "카카오 사용자 정보 조회 실패", error)
                return@me
            }

            val account = user?.kakaoAccount
            Log.d(
                "MyInfoFragment",
                "me 성공 userId=${user?.id}, profileNeedsAgreement=${account?.profileNeedsAgreement}, emailNeedsAgreement=${account?.emailNeedsAgreement}"
            )

            if (account?.profileNeedsAgreement == true || account?.emailNeedsAgreement == true) {
                Log.d("MyInfoFragment", "추가 동의 필요 -> loginWithNewScopes 호출")
                requestKakaoProfileScopes()
                return@me
            }
            val profile = account?.profile

            Log.d(
                "MyInfoFragment",
                "profile 값 nickname=${profile?.nickname}, email=${account?.email}"
            )
            currentBinding.tvUserName.text = profile?.nickname ?: "카카오 사용자"
            currentBinding.tvKakaoEmail.text = account?.email ?: "카카오 계정 연동"

            val imageUrl = profile?.thumbnailImageUrl ?: profile?.profileImageUrl
            if (imageUrl.isNullOrBlank()) {
                Log.d("MyInfoFragment", "프로필 이미지 URL 없음(기본 이미지 유지)")
                return@me
            }
            Log.d("MyInfoFragment", "프로필 이미지 URL=$imageUrl")

            // Coil 적용
            currentBinding.ivKakaoProfile.load(imageUrl) {
                crossfade(true) // 부드럽게 나타나는 효과
                listener(
                    onSuccess = { _, _ -> Log.d("MyInfoFragment", "Coil: 프로필 이미지 적용 완료") },
                    onError = { _, _ -> Log.e("MyInfoFragment", "Coil: 프로필 이미지 로드 실패") }
                )
            }
        }
    }

    private fun requestKakaoProfileScopes() {
        val scopes = listOf("profile_nickname", "profile_image", "account_email")
        UserApiClient.Companion.instance.loginWithNewScopes(requireActivity(), scopes) { _, error ->
            if (error != null) {
                Log.e("MyInfoFragment", "카카오 추가 동의 요청 실패", error)
                return@loginWithNewScopes
            }
            Log.d("MyInfoFragment", "카카오 추가 동의 성공 -> 프로필 재조회")
            fetchKakaoProfile()
        }
    }

    private fun bindLogoutAction() {
        binding.menuLogout.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.alpha = 0.65f
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> v.alpha = 1.0f
            }
            false
        }

        binding.menuLogout.setOnClickListener {
            Toast.makeText(requireContext(), "로그아웃 처리 중...", Toast.LENGTH_SHORT).show()
            UserApiClient.Companion.instance.logout { error ->
                if (error != null) {
                    Log.e("MyInfoFragment", "카카오 로그아웃 실패", error)
                    Toast.makeText(requireContext(), "로그아웃 실패", Toast.LENGTH_SHORT).show()
                    return@logout
                }

                context?.let { UserSession.clear(it) }
                Toast.makeText(requireContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                moveToLogin()
            }
        }

        binding.menuLogout.setOnLongClickListener {
            Toast.makeText(requireContext(), "연결 끊기 처리 중...", Toast.LENGTH_SHORT).show()
            UserApiClient.Companion.instance.unlink { error ->
                if (error != null) {
                    Log.e("MyInfoFragment", "카카오 연결 끊기(unlink) 실패", error)
                    Toast.makeText(requireContext(), "연결 끊기 실패", Toast.LENGTH_SHORT).show()
                    return@unlink
                }

                Log.d("MyInfoFragment", "카카오 연결 끊기(unlink) 성공")
                UserSession.clear(requireContext())
                Toast.makeText(requireContext(), "연결 끊기 완료(재로그인 필요)", Toast.LENGTH_SHORT).show()
                moveToLogin()
            }
            true
        }
    }

    private fun moveToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}