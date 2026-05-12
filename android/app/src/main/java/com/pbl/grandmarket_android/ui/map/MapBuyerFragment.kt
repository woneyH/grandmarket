package com.pbl.grandmarket_android.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.pbl.grandmarket_android.util.KakaoMapSupport
import com.pbl.grandmarket_android.R
import com.pbl.grandmarket_android.ui.map.StoreListBottomSheetFragment
import com.pbl.grandmarket_android.data.model.StoreItem
import com.pbl.grandmarket_android.databinding.FragmentMapBuyerBinding

class MapBuyerFragment : Fragment() {
    private var _binding: FragmentMapBuyerBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var kakaoMap: KakaoMap? = null

    // 검색 기준 좌표 (XML의 layoutCenterPin 위치)
    private var myLat: Double = 35.1537
    private var myLng: Double = 128.1022

    // 검색 반경 15km (15,000m)
    private val SEARCH_RADIUS = 15000f

    // 현재 반경 내 점포 리스트
    private val storeList = mutableListOf<StoreItem>()

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBuyerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        startMap()

        // 1. 내 위치 버튼 (우측 하단)
        binding.fabCurrentLocation.setOnClickListener {
            checkLocationPermission()
        }

        // "현 위치에서 점포 검색" 버튼 클릭 이벤트
        binding.btnSearchHere.setOnClickListener {
            // 카카오맵의 현재 화면 중앙 좌표를 가져옵니다.
            val centerPosition = kakaoMap?.cameraPosition?.position
            if (centerPosition != null) {
                myLat = centerPosition.latitude
                myLng = centerPosition.longitude

                Toast.makeText(requireContext(), "이 위치 주변을 검색합니다.", Toast.LENGTH_SHORT).show()
                // 데이터 다시 불러오고 거리 재계산
                fetchStoreLocationsFromFirestore()
            }
        }

        // 3. 하단 리스트 보기 팝업 버튼
        binding.btnShowStoreList.setOnClickListener {
            if (storeList.isEmpty()) {
                Toast.makeText(requireContext(), "주변 반경 20km 내에 점포가 없습니다.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                val bottomSheet = StoreListBottomSheetFragment(storeList)
                bottomSheet.show(parentFragmentManager, "StoreListBottomSheet")
            }
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
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    myLat = location.latitude
                    myLng = location.longitude

                    kakaoMap?.moveCamera(
                        CameraUpdateFactory.newCenterPosition(LatLng.from(myLat, myLng)),
                        CameraAnimation.from(500, true, true)
                    )

                    // 처음 내 위치를 잡았을 때 1회 자동 검색 실행
                    fetchStoreLocationsFromFirestore()
                }
            }
    }

    // DB에서 점포를 가져오고 현재 myLat, myLng 기준으로 거리 계산 및 마커 표시
    private fun fetchStoreLocationsFromFirestore() {
        FirebaseFirestore.getInstance().collection("storeLocation")
            .get()
            .addOnSuccessListener { documents ->
                storeList.clear()

                val labelManager = kakaoMap?.labelManager
                val storeLayer = labelManager?.getLayer("storeLayer") ?: labelManager?.addLayer(
                    LabelLayerOptions.from("storeLayer")
                )

                // 기존 마커 모두 초기화
                storeLayer?.removeAll()

                for (document in documents) {
                    val lat = document.getDouble("latitude") ?: continue
                    val lng = document.getDouble("longitude") ?: continue
                    val nickname = document.getString("nickname") ?: "이름 없음"
                    val address = document.getString("address") ?: ""

                    val distance = calculateDistance(myLat, myLng, lat, lng)

                    // 15km 이내만 필터링
                    if (distance <= SEARCH_RADIUS) {
                        storeList.add(StoreItem(document.id, nickname, address, lat, lng, distance))

                        addStoreMarkerOnMap(storeLayer, lat, lng, nickname)
                    }
                }

                // 가까운 순으로 정렬
                storeList.sortBy { it.distanceToMe }

                // 버튼 텍스트 업데이트
                binding.btnShowStoreList.text = "주변 점포 ${storeList.size}개 보기"

                if (storeList.isNotEmpty()) {
                    Log.d(
                        "MapBuyer",
                        "가장 가까운 점포: ${storeList[0].storeName}, 거리: ${storeList[0].getFormattedDistance()}"
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapBuyer", "점포 데이터 불러오기 실패", e)
            }
    }

    private fun addStoreMarkerOnMap(
        layer: LabelLayer?,
        lat: Double,
        lng: Double,
        storeName: String
    ) {
        try {
            val labelManager = kakaoMap?.labelManager ?: return

            val labelText = LabelTextBuilder().setTexts(storeName)

            val style = LabelStyles.from(
                LabelStyle.from(R.drawable.store_marker)
                    .setTextStyles(18, ContextCompat.getColor(requireContext(), R.color.black))
            )

            val registeredStyle = labelManager.addLabelStyles(style)

            val options = LabelOptions.from(LatLng.from(lat, lng))
                .setStyles(registeredStyle)
                .setTexts(labelText)

            layer?.addLabel(options)

        } catch (e: Exception) {
            Log.e("MapBuyer", "마커 표시 에러", e)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun startMap() {
        binding.mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() = Unit
                override fun onMapError(error: Exception?) {
                    Log.e("MapBuyer", "Kakao map error", error)
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map
                    checkLocationPermission()
                }

                override fun getPosition(): LatLng = LatLng.from(myLat, myLng)
                override fun getZoomLevel(): Int = 15
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (KakaoMapSupport.isSupportedAbi) _binding?.mapView?.resume()
    }

    override fun onPause() {
        if (KakaoMapSupport.isSupportedAbi) _binding?.mapView?.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        if (KakaoMapSupport.isSupportedAbi) _binding?.mapView?.pause()
        super.onDestroyView()
        _binding = null
    }
}