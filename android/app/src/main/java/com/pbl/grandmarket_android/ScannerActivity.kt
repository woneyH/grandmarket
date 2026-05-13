package com.pbl.grandmarket_android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.pbl.grandmarket_android.ai.OverlayView
import com.pbl.grandmarket_android.ai.YoloDetector
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var overlay: OverlayView
    private lateinit var btnRegister: Button
    private lateinit var tvDetectedInfo: TextView

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var yoloDetector: YoloDetector

    private var highestConfidenceClass: String? = null

    private lateinit var btnGallery: Button

    private val galleryLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            val results = yoloDetector.detect(bitmap)
            
            overlay.setResults(results, bitmap.width, bitmap.height)
            
            if (results.isNotEmpty()) {
                val best = results.maxByOrNull { it.confidence }
                if (best != null && best.confidence > 0.6f) {
                    highestConfidenceClass = best.className
                    tvDetectedInfo.text = "앨범 사진 인식됨: ${best.className} (${(best.confidence * 100).toInt()}%)"
                    btnRegister.visibility = View.VISIBLE
                } else {
                    btnRegister.visibility = View.GONE
                    tvDetectedInfo.text = "상품을 인식할 수 없습니다."
                }
            } else {
                btnRegister.visibility = View.GONE
                tvDetectedInfo.text = "상품을 인식할 수 없습니다."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        viewFinder = findViewById(R.id.viewFinder)
        overlay = findViewById(R.id.overlay)
        btnRegister = findViewById(R.id.btnRegister)
        tvDetectedInfo = findViewById(R.id.tvDetectedInfo)
        btnGallery = findViewById(R.id.btnGallery)

        yoloDetector = YoloDetector(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            androidx.core.app.ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        btnRegister.setOnClickListener {
            highestConfidenceClass?.let { className ->
                val intent = Intent(this, ProductRegistrationActivity::class.java)
                intent.putExtra("DETECTED_CLASS", className)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                tvDetectedInfo.text = "카메라 권한이 필요합니다."
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // 특정 방향(전/후면)을 따지지 않고 사용 가능한 아무 카메라나 가져옵니다
                val anyCameraSelector = CameraSelector.Builder().build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, anyCameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("ScannerActivity", "All cameras failed", exc)
                runOnUiThread {
                    tvDetectedInfo.text = "가상기기 카메라 에러: ${exc.message}"
                }
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val bitmap = imageProxy.toBitmap()
            
            // Fix rotation if needed
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            // Run inference
            val results = yoloDetector.detect(rotatedBitmap)

            // Update UI on main thread
            runOnUiThread {
                overlay.setResults(results, rotatedBitmap.width, rotatedBitmap.height)
                
                if (results.isNotEmpty()) {
                    // Find highest confidence
                    val best = results.maxByOrNull { it.confidence }
                    if (best != null && best.confidence > 0.6f) {
                        highestConfidenceClass = best.className
                        tvDetectedInfo.text = "인식됨: ${best.className} (${(best.confidence * 100).toInt()}%)"
                        btnRegister.visibility = View.VISIBLE
                    } else {
                        btnRegister.visibility = View.GONE
                        tvDetectedInfo.text = "화면에 상품을 비춰주세요"
                    }
                } else {
                    btnRegister.visibility = View.GONE
                    tvDetectedInfo.text = "화면에 상품을 비춰주세요"
                }
            }
        }
        imageProxy.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
