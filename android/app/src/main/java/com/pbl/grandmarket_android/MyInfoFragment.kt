package com.pbl.grandmarket_android

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kakao.sdk.user.UserApiClient
import com.pbl.grandmarket_android.databinding.FragmentMyInfoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

class MyInfoFragment : Fragment() {
    private var _binding: FragmentMyInfoBinding? = null
    private val binding get() = _binding!!

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
        // [수정] 로그아웃 메뉴 클릭 시 카카오 로그아웃 + 세션 정리 + 로그인 화면 이동
        bindLogoutAction()
        fetchKakaoProfile()
    }

    private fun fetchKakaoProfile() {
        // [수정] MyInfo 진입 시 카카오 사용자 정보 조회 시작 로그
        Log.d("MyInfoFragment", "fetchKakaoProfile() 호출")
        UserApiClient.instance.me { user, error ->
            val currentBinding = _binding ?: return@me

            if (error != null) {
                Log.e("MyInfoFragment", "카카오 사용자 정보 조회 실패", error)
                return@me
            }

            val account = user?.kakaoAccount
            // [수정] 동의 필요 여부를 로그로 남겨 원인 추적
            Log.d(
                "MyInfoFragment",
                "me 성공 userId=${user?.id}, profileNeedsAgreement=${account?.profileNeedsAgreement}, emailNeedsAgreement=${account?.emailNeedsAgreement}"
            )
            // [수정] 프로필/이메일 동의가 필요한 경우 추가 동의 화면을 먼저 요청
            if (account?.profileNeedsAgreement == true || account?.emailNeedsAgreement == true) {
                Log.d("MyInfoFragment", "추가 동의 필요 -> loginWithNewScopes 호출")
                requestKakaoProfileScopes()
                return@me
            }
            val profile = account?.profile

            // [수정] 닉네임/이메일 실제 값 로그
            Log.d(
                "MyInfoFragment",
                "profile 값 nickname=${profile?.nickname}, email=${account?.email}"
            )
            currentBinding.tvUserName.text = profile?.nickname ?: "카카오 사용자"
            currentBinding.tvKakaoEmail.text = account?.email ?: "카카오 계정 연동"

            val imageUrl = profile?.thumbnailImageUrl ?: profile?.profileImageUrl
            if (imageUrl.isNullOrBlank()) {
                // [수정] 이미지 URL이 비어 기본 프로필 유지되는 경우 로그
                Log.d("MyInfoFragment", "프로필 이미지 URL 없음(기본 이미지 유지)")
                return@me
            }
            Log.d("MyInfoFragment", "프로필 이미지 URL=$imageUrl")

            viewLifecycleOwner.lifecycleScope.launch {
                val bitmap = loadBitmap(imageUrl)
                if (bitmap != null && _binding != null) {
                    _binding?.ivKakaoProfile?.setImageBitmap(bitmap)
                    // [수정] 이미지 적용 성공 로그
                    Log.d("MyInfoFragment", "프로필 이미지 적용 완료")
                } else {
                    // [수정] 이미지 디코드 실패 분기 로그
                    Log.d("MyInfoFragment", "프로필 이미지 디코드 실패 또는 뷰 소멸")
                }
            }
        }
    }

    // [수정] 카카오 사용자 이름/프로필 이미지를 받기 위한 추가 동의 요청
    private fun requestKakaoProfileScopes() {
        val scopes = listOf("profile_nickname", "profile_image", "account_email")
        UserApiClient.instance.loginWithNewScopes(requireActivity(), scopes) { _, error ->
            if (error != null) {
                Log.e("MyInfoFragment", "카카오 추가 동의 요청 실패", error)
                return@loginWithNewScopes
            }
            // [수정] 추가 동의 성공 후 재조회 로그
            Log.d("MyInfoFragment", "카카오 추가 동의 성공 -> 프로필 재조회")
            fetchKakaoProfile()
        }
    }

    // [수정] 로그아웃 버튼 동작 연결 (짧게 클릭: logout, 길게 클릭: unlink 테스트)
    private fun bindLogoutAction() {
        // [수정] 눌림 시 즉시 시각 피드백(살짝 어둡게)
        binding.menuLogout.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> v.alpha = 0.65f
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> v.alpha = 1.0f
            }
            false
        }

        binding.menuLogout.setOnClickListener {
            // [수정] 클릭 즉시 처리중 안내
            Toast.makeText(requireContext(), "로그아웃 처리 중...", Toast.LENGTH_SHORT).show()
            UserApiClient.instance.logout { error ->
                if (error != null) {
                    Log.e("MyInfoFragment", "카카오 로그아웃 실패", error)
                    Toast.makeText(requireContext(), "로그아웃 실패", Toast.LENGTH_SHORT).show()
                    return@logout
                }

                UserSession.clear(requireContext())
                Toast.makeText(requireContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                moveToLogin()
            }
        }

        binding.menuLogout.setOnLongClickListener {
            // [수정] 길게 눌렀을 때 unlink 처리 시작 안내
            Toast.makeText(requireContext(), "연결 끊기 처리 중...", Toast.LENGTH_SHORT).show()
            UserApiClient.instance.unlink { error ->
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

    // [수정] 로그아웃/연결끊기 후 공통 화면 이동 처리
    private fun moveToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private suspend fun loadBitmap(imageUrl: String) = withContext(Dispatchers.IO) {
        try {
            URL(imageUrl).openStream().use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: IOException) {
            Log.e("MyInfoFragment", "프로필 이미지 로드 실패", e)
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
