package com.pbl.grandmarket_android.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.nio.FloatBuffer
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

class YoloDetector(context: Context) {
    private val env = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null
    private var lastErrorMessage: String? = null // 마지막 에러 메시지 저장용

    val classNames = arrayOf(
        "아보카도", "콩", "비트", "피망", "브로콜리", "방울양배추",
        "배추", "당근", "콜리플라워", "샐러리", "옥수수", "오이",
        "가지", "강낭콩", "마늘", "매운고추", "양파", "완두콩",
        "감자", "호박", "래디시", "무", "상추", "패티슨호박",
        "토마토", "애호박", "두부", "생선"
    )

    init {
        loadModel(context)
    }

    private fun loadModel(context: Context) {
        try {
            Log.d("YoloDetector", "--- 🚀 모델 로드 단계 시작 ---")

            // 1. Assets 폴더 내 모든 파일 목록 출력 (파일명 확인용)
            val assetsList = context.assets.list("") ?: emptyArray()
            Log.d("YoloDetector", "📁 Assets 폴더 파일 목록: ${assetsList.joinToString(", ")}")

            val modelFileName = "expanded_28classes.onnx"

            if (!assetsList.contains(modelFileName)) {
                val error = "❌ [에러] Assets에 '$modelFileName' 파일이 없습니다. 대소문자나 오타를 확인하세요."
                Log.e("YoloDetector", error)
                lastErrorMessage = error
                return
            }

            // 2. 파일 읽기 시도
            Log.d("YoloDetector", "✅ '$modelFileName' 발견. 읽기 시작...")
            val inputStream = context.assets.open(modelFileName)
            val modelBytes = inputStream.readBytes()
            inputStream.close()
            Log.d("YoloDetector", "📖 파일 읽기 성공! 크기: ${modelBytes.size} bytes")

            // 3. ONNX 세션 생성
            val options = OrtSession.SessionOptions()
            session = env.createSession(modelBytes, options)
            Log.d("YoloDetector", "🎉 ONNX 세션 생성 완료! 이제 분석이 가능합니다.")

        } catch (e: Exception) {
            val error = "❌ 모델 로드 중 예외 발생: ${e.message}"
            Log.e("YoloDetector", error, e)
            lastErrorMessage = error
        }
    }

    data class Detection(
        val className: String,
        val confidence: Float,
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float
    )

    fun detect(bitmap: Bitmap): List<Detection> {
        val s = session ?: run {
            Log.e("YoloDetector", "⚠️ [분석 불가] 세션이 없습니다. 원인: $lastErrorMessage")
            return emptyList()
        }

        Log.d("YoloDetector", "🏃 분석 시작 (이미지 크기: ${bitmap.width}x${bitmap.height})")

        val inputSize = 640
        val scale = min(inputSize.toFloat() / bitmap.width, inputSize.toFloat() / bitmap.height)
        val newW = Math.round(bitmap.width * scale)
        val newH = Math.round(bitmap.height * scale)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true)

        val padW = (inputSize - newW) / 2
        val padH = (inputSize - newH) / 2

        val floatBuffer = FloatBuffer.allocate(3 * inputSize * inputSize)
        val pixels = IntArray(newW * newH)
        scaledBitmap.getPixels(pixels, 0, newW, 0, 0, newW, newH)

        for (i in 0 until 3 * inputSize * inputSize) {
            floatBuffer.put(i, 0.5f)
        }

        var pixelIndex = 0
        for (y in 0 until newH) {
            for (x in 0 until newW) {
                val p = pixels[pixelIndex++]
                floatBuffer.put(0 * inputSize * inputSize + (y + padH) * inputSize + (x + padW), ((p shr 16) and 0xFF) / 255.0f)
                floatBuffer.put(1 * inputSize * inputSize + (y + padH) * inputSize + (x + padW), ((p shr 8) and 0xFF) / 255.0f)
                floatBuffer.put(2 * inputSize * inputSize + (y + padH) * inputSize + (x + padW), (p and 0xFF) / 255.0f)
            }
        }
        floatBuffer.rewind()

        val inputName = s.inputNames.iterator().next()
        val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())

        try {
            val tensor = OnnxTensor.createTensor(env, floatBuffer, shape)
            val output = s.run(Collections.singletonMap(inputName, tensor))
            val resultTensor = output[0] as OnnxTensor
            val resultData = resultTensor.floatBuffer

            val numBoxes = 8400
            val numClasses = 28
            val detections = mutableListOf<Detection>()

            var maxConfidenceFound = 0f

            for (i in 0 until numBoxes) {
                var maxConf = 0f
                var maxClassIndex = -1

                for (c in 0 until numClasses) {
                    val conf = resultData.get((4 + c) * numBoxes + i)
                    if (conf > maxConf) {
                        maxConf = conf
                        maxClassIndex = c
                    }
                }

                if (maxConf > maxConfidenceFound) maxConfidenceFound = maxConf

                if (maxConf > 0.3f) { // 테스트를 위해 임계값 0.3
                    val cx = resultData.get(0 * numBoxes + i)
                    val cy = resultData.get(1 * numBoxes + i)
                    val w = resultData.get(2 * numBoxes + i)
                    val h = resultData.get(3 * numBoxes + i)

                    val x1 = ((cx - w / 2) - padW) / scale
                    val y1 = ((cy - h / 2) - padH) / scale
                    val x2 = ((cx + w / 2) - padW) / scale
                    val y2 = ((cy + h / 2) - padH) / scale

                    detections.add(Detection(classNames[maxClassIndex], maxConf, max(0f, x1), max(0f, y1), min(bitmap.width.toFloat(), x2), min(bitmap.height.toFloat(), y2)))
                }
            }

            Log.d("YoloDetector", "🔍 분석 결과 - 최고 확률: $maxConfidenceFound, 검출 수: ${detections.size}")

            tensor.close()
            output.close()
            return applyNMS(detections, 0.45f)

        } catch (e: Exception) {
            Log.e("YoloDetector", "❌ 추론 중 오류 발생", e)
            return emptyList()
        }
    }

    private fun applyNMS(boxes: List<Detection>, iouThreshold: Float): List<Detection> {
        val sortedBoxes = boxes.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<Detection>()

        while (sortedBoxes.isNotEmpty()) {
            val current = sortedBoxes.removeAt(0)
            selected.add(current)
            sortedBoxes.removeAll { box ->
                current.className == box.className && calculateIoU(current, box) > iouThreshold
            }
        }
        return selected
    }

    private fun calculateIoU(box1: Detection, box2: Detection): Float {
        val x1 = max(box1.x1, box2.x1)
        val y1 = max(box1.y1, box2.y1)
        val x2 = min(box1.x2, box2.x2)
        val y2 = min(box1.y2, box2.y2)
        val intersectionArea = max(0f, x2 - x1) * max(0f, y2 - y1)
        val box1Area = (box1.x2 - box1.x1) * (box1.y2 - box1.y1)
        val box2Area = (box2.x2 - box2.x1) * (box2.y2 - box2.y1)
        val totalArea = box1Area + box2Area - intersectionArea
        return if (totalArea <= 0) 0f else intersectionArea / totalArea
    }
}