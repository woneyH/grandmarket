package com.pbl.grandmarket_android

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        fetchKakaoProfile()
    }

    private fun fetchKakaoProfile() {
        UserApiClient.instance.me { user, error ->
            val currentBinding = _binding ?: return@me

            if (error != null) {
                Log.e("MyInfoFragment", "카카오 사용자 정보 조회 실패", error)
                return@me
            }

            val account = user?.kakaoAccount
            val profile = account?.profile

            currentBinding.tvUserName.text = profile?.nickname ?: "카카오 사용자"
            currentBinding.tvKakaoEmail.text = account?.email ?: "카카오 계정 연동"

            val imageUrl = profile?.thumbnailImageUrl ?: profile?.profileImageUrl
            if (imageUrl.isNullOrBlank()) return@me

            viewLifecycleOwner.lifecycleScope.launch {
                val bitmap = loadBitmap(imageUrl)
                if (bitmap != null && _binding != null) {
                    _binding?.ivKakaoProfile?.setImageBitmap(bitmap)
                }
            }
        }
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
