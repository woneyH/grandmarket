package com.pbl.grandmarket_android

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.pbl.grandmarket_android.databinding.FragmentMapBinding
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback

class MapFragment : Fragment() {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!KakaoMapSupport.isSupportedAbi) {
            binding.tvSelectedAddress.text = "현재 에뮬레이터(x86)에서는 지도 미지원입니다. 실기기 또는 ARM 에뮬레이터를 사용하세요."
            Toast.makeText(
                requireContext(),
                "카카오맵은 ARM 기기/에뮬레이터에서만 동작합니다.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        startMap()
    }

    override fun onResume() {
        super.onResume()
        if (KakaoMapSupport.isSupportedAbi) {
            _binding?.mapView?.resume()
        }
    }

    override fun onPause() {
        if (KakaoMapSupport.isSupportedAbi) {
            _binding?.mapView?.pause()
        }
        super.onPause()
    }

    private fun startMap() {
        binding.mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() = Unit

                override fun onMapError(error: Exception?) {
                    Log.e("MapFragment", "Kakao map error", error)
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(kakaoMap: KakaoMap) {
                    Log.d("MapFragment", "Kakao map ready")
                }

                override fun getPosition(): LatLng {
                    return LatLng.from(37.5665, 126.9780) // 서울시청
                }

                override fun getZoomLevel(): Int = 15
            }
        )
    }

    override fun onDestroyView() {
        if (KakaoMapSupport.isSupportedAbi) {
            _binding?.mapView?.pause()
        }
        super.onDestroyView()
        _binding = null
    }
}
