package com.pbl.grandmarket_android.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kakao.sdk.user.UserApiClient
import android.graphics.Matrix
import android.media.ExifInterface
import com.pbl.grandmarket_android.R
import com.pbl.grandmarket_android.aiimport.YoloDetector
import com.pbl.grandmarket_android.data.model.UserRole
import com.pbl.grandmarket_android.data.local.UserSession
import com.pbl.grandmarket_android.ui.item_list.RegisterItemBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var introMessageText: TextView? = null
    private var btnSearchRegister: View? = null
    private var homeSearchBar: View? = null
    private var btnCameraRegister: View? = null

    private lateinit var yoloDetector: YoloDetector

    // 고해상도 사진을 위한 변수들
    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null

    // 1. 카메라 권한 요청 런처
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

    // 2. 카메라 촬영 결과 처리 런처 (고해상도 파일 읽기)
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                currentPhotoPath?.let { path ->
                    // 1. 이미지 읽기
                    var bitmap = BitmapFactory.decodeFile(path)
                    
                    // 2. 사진 회전 각도 확인 및 보정 (추가된 로직)
                    bitmap = rotateImageIfRequired(bitmap, path)
                    
                    bitmap?.let {
                        runYoloDetection(it)
                    }
                }
            }
        }

    // 사진의 회전 정보를 읽어 정방향으로 돌려주는 함수
    private fun rotateImageIfRequired(img: Bitmap, path: String): Bitmap {
        val ei = ExifInterface(path)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        if (rotatedImg != img) img.recycle() // 원본 비트맵 메모리 해제
        return rotatedImg
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val role = UserSession.getRole(requireContext())
        val layoutRes = if (role == UserRole.SELLER) R.layout.fragment_home else R.layout.fragment_home_buyer
        val view = inflater.inflate(layoutRes, container, false)

        introMessageText = view.findViewById(R.id.intro_message_text)
        if (role == UserRole.SELLER) {
            btnSearchRegister = view.findViewById(R.id.btnSearchRegister)
            homeSearchBar = view.findViewById(R.id.home_search_bar)
            btnCameraRegister = view.findViewById(R.id.btnCameraRegister)
        }

        yoloDetector = YoloDetector(requireContext())
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchKakaoProfile()

        btnSearchRegister?.setOnClickListener { showRegisterBottomSheet() }
        homeSearchBar?.setOnClickListener { showRegisterBottomSheet() }
        btnCameraRegister?.setOnClickListener { checkCameraPermission() }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        // 임시 사진 파일 생성
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            null
        }

        photoFile?.also {
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider", // FileProvider 권한 설정 필요
                it
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri) // 고해상도 저장을 위해 URI 전달
            takePictureLauncher.launch(intent)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun runYoloDetection(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.Default) {
            val results = yoloDetector.detect(bitmap)
            withContext(Dispatchers.Main) {
                if (results.isNotEmpty()) {
                    val topResult = results[0]
                    // 사용자 요청대로 인식 결과만 토스트로 출력
                    Toast.makeText(requireContext(), "인식 결과: ${topResult.className}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "인식된 채소가 없습니다. 다시 찍어주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showRegisterBottomSheet() {
        val bottomSheet = RegisterItemBottomSheet()
        bottomSheet.show(childFragmentManager, "RegisterItemBottomSheet")
    }

    private fun fetchKakaoProfile() {
        UserApiClient.instance.me { user, error ->
            val currentTextView = introMessageText ?: return@me
            if (error == null) {
                val nickname = user?.kakaoAccount?.profile?.nickname ?: "사용자"
                currentTextView.text = "${currentTextView.text} $nickname 님 🖐️"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        introMessageText = null
        btnSearchRegister = null
        homeSearchBar = null
        btnCameraRegister = null
    }
}