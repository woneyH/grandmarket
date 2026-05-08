package com.pbl.grandmarket_android

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.sdk.user.UserApiClient
import com.pbl.grandmarket_android.databinding.FragmentMapBinding
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.pbl.grandmarket_android.repository.StoreLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MapFragment : Fragment() {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var kakaoMap: KakaoMap? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted || coarseLocationGranted) {
                getCurrentLocationAndUpdateMap()
            } else {
                Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocationAndUpdateMap()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndUpdateMap() {
        binding.tvSelectedAddress.text = "위치 탐색 중..." // 로딩 메시지

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    kakaoMap?.moveCamera(
                        CameraUpdateFactory.newCenterPosition(LatLng.from(latitude, longitude)),
                        CameraAnimation.from(500, true, true)
                    )
                    Log.d("MapFragment", "정확한 현재 위치로 이동 완료: $latitude, $longitude")

                } else {
                    binding.tvSelectedAddress.text = "위치를 찾을 수 없습니다."
                    Toast.makeText(
                        requireContext(),
                        "위치를 찾을 수 없습니다. GPS를 확인해주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // 위도, 경도를 한글 주소로 변환하여 TextView에 반영하는 함수
    private fun updateAddressText(latitude: Double, longitude: Double) {
        binding.tvSelectedAddress.text = "주소 변환 중..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.KOREA)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // API 33 이상 (비동기 방식)
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            // "대한민국 "을 제거하여 깔끔하게 표시
                            val addressText = addresses[0].getAddressLine(0).replace("대한민국 ", "")
                            requireActivity().runOnUiThread {
                                binding.tvSelectedAddress.text = addressText
                            }
                        }
                    }
                } else {
                    // API 33 미만 (동기 방식)
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val addressText = if (!addresses.isNullOrEmpty()) {
                        addresses[0].getAddressLine(0).replace("대한민국 ", "")
                    } else {
                        "주소를 찾을 수 없습니다."
                    }

                    withContext(Dispatchers.Main) {
                        binding.tvSelectedAddress.text = addressText
                    }
                }
            } catch (e: Exception) {
                Log.e("MapFragment", "주소 변환 오류", e)
                withContext(Dispatchers.Main) {
                    binding.tvSelectedAddress.text = "주소를 불러오지 못했습니다."
                }
            }
        }
    }

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        startMap()

        binding.fabCurrentLocation.setOnClickListener {
            checkLocationPermission()
        }

        binding.btnCurrentLocation.setOnClickListener {
            checkLocationPermission()
        }

        setLocationRegistration()
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
                override fun onMapReady(map: KakaoMap) {
                    Log.d("MapFragment", "Kakao map ready")
                    kakaoMap = map

                    kakaoMap?.setOnCameraMoveEndListener { _, cameraPosition, _ ->
                        val centerPosition = cameraPosition.position
                        // 멈춘 시점의 위도, 경도로 주소 업데이트
                        updateAddressText(centerPosition.latitude, centerPosition.longitude)
                    }

                    checkLocationPermission()
                }

                override fun getPosition(): LatLng {
                    return LatLng.from(35.1537, 128.1022)
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


    private fun setLocationRegistration() {
        binding.btnRegisterLocation.setOnClickListener {
            UserApiClient.instance.me { user, error ->
                if(error != null) {
                    Log.e("Kakao", "사용자 정보 요청 실패", error)
                    return@me
                }

                if(user != null) {
                    val kakaoId = user.id
                    val nickname = user.kakaoAccount?.profile?.nickname ?: "이름 없음"

                    //지도의 중앙 좌표값
                    val centerPos = kakaoMap?.cameraPosition?.position
                    val lat = centerPos?.latitude
                    val lng = centerPos?.longitude
                    val address = binding.tvSelectedAddress.text.toString()

                    val storeLocation = StoreLocation(
                        kakaoId = kakaoId,
                        nickname = nickname,
                        latitude = lat,
                        longitude = lng,
                        address = address
                    )

                    FirebaseFirestore.getInstance().collection("storeLocation")
                        .document(kakaoId.toString())
                        .set(storeLocation)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(),"점포 등록되었습니다!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(),"점포 등록 실패", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }
}