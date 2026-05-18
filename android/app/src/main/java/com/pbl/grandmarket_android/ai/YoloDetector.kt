package com.pbl.grandmarket_android.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import java.nio.FloatBuffer
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

class YoloDetector(context: Context) {
    private val env = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null

    // 28 classes (순서는 학습 시 사용한 클래스 순서와 동일해야 함)
    val classNames = arrayOf(
        "아보카도", "콩", "비트", "피망", "브로콜리", "방울양배추",
        "배추", "당근", "콜리플라워", "샐러리", "옥수수", "오이",
        "가지", "강낭콩", "마늘", "매운고추", "양파", "완두콩",
        "감자", "호박", "래디시", "무", "상추", "패티슨호박",
        "토마토", "애호박", "두부", "생선"
    )

    companion object {
        private const val TAG = "YoloDetector"
        private const val INPUT_SIZE = 640
        // Ultralytics 기본 letterbox 패딩 색상: 114/255 ≈ 0.447
        private const val LETTERBOX_PAD_VALUE = 114f / 255f
        // ONNX 모델은 PyTorch보다 confidence가 낮게 나오므로 threshold를 낮춤
        private const val CONFIDENCE_THRESHOLD = 0.25f
        private const val NMS_IOU_THRESHOLD = 0.45f
    }

    init {
        try {
            val modelBytes = context.assets.open("expanded_28classes.onnx").readBytes()
            val options = OrtSession.SessionOptions()
            session = env.createSession(modelBytes, options)
            Log.d(TAG, "ONNX Model loaded successfully")

            // 모델 입출력 shape 로그 출력
            session?.let { s ->
                s.inputInfo.forEach { (name, info) ->
                    Log.d(TAG, "Input: $name -> ${info.info}")
                }
                s.outputInfo.forEach { (name, info) ->
                    Log.d(TAG, "Output: $name -> ${info.info}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load ONNX model", e)
        }
    }

    data class Detection(
        val className: String,
        val confidence: Float,
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float
    )

    fun detect(bitmap: Bitmap): List<Detection> {
        val s = session ?: return emptyList()

        // Bitmap을 ARGB_8888로 강제 변환 (RGB_565 등에서 색상 손실 방지)
        val argbBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        // 1. Letterbox 리사이즈 (비율 유지 + 패딩)
        val scale = min(INPUT_SIZE.toFloat() / argbBitmap.width, INPUT_SIZE.toFloat() / argbBitmap.height)
        val newW = Math.round(argbBitmap.width * scale)
        val newH = Math.round(argbBitmap.height * scale)
        val scaledBitmap = Bitmap.createScaledBitmap(argbBitmap, newW, newH, true)

        val padW = (INPUT_SIZE - newW) / 2
        val padH = (INPUT_SIZE - newH) / 2

        // 2. FloatBuffer 생성 [1, 3, 640, 640] — Ultralytics와 동일한 전처리
        val floatBuffer = FloatBuffer.allocate(3 * INPUT_SIZE * INPUT_SIZE)
        val pixels = IntArray(newW * newH)
        scaledBitmap.getPixels(pixels, 0, newW, 0, 0, newW, newH)

        // 패딩 영역을 Ultralytics 기본 색상(114/255)으로 초기화
        for (i in 0 until 3 * INPUT_SIZE * INPUT_SIZE) {
            floatBuffer.put(i, LETTERBOX_PAD_VALUE)
        }

        // 실제 이미지 픽셀을 RGB 순서로 채움 (Bitmap ARGB → RGB 정규화)
        var pixelIndex = 0
        for (y in 0 until newH) {
            for (x in 0 until newW) {
                val p = pixels[pixelIndex++]
                val r = ((p shr 16) and 0xFF) / 255.0f
                val g = ((p shr 8) and 0xFF) / 255.0f
                val b = (p and 0xFF) / 255.0f

                val destY = y + padH
                val destX = x + padW

                floatBuffer.put(0 * INPUT_SIZE * INPUT_SIZE + destY * INPUT_SIZE + destX, r)
                floatBuffer.put(1 * INPUT_SIZE * INPUT_SIZE + destY * INPUT_SIZE + destX, g)
                floatBuffer.put(2 * INPUT_SIZE * INPUT_SIZE + destY * INPUT_SIZE + destX, b)
            }
        }
        floatBuffer.rewind()

        // 3. ONNX 추론
        val inputName = s.inputNames.iterator().next()
        val shape = longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())

        try {
            val tensor = OnnxTensor.createTensor(env, floatBuffer, shape)
            val output = s.run(Collections.singletonMap(inputName, tensor))
            val resultTensor = output[0] as OnnxTensor
            val resultData = resultTensor.floatBuffer

            // 출력 shape를 런타임에서 동적으로 읽기 (하드코딩 제거)
            val outputShape = resultTensor.info.shape  // [1, numChannels, numBoxes]
            val numChannels = outputShape[1].toInt()    // 4(bbox) + numClasses
            val numBoxes = outputShape[2].toInt()       // 보통 8400
            val numClasses = numChannels - 4            // 28

            Log.d(TAG, "Output shape: [1, $numChannels, $numBoxes], classes=$numClasses")

            val detections = mutableListOf<Detection>()

            // 4. 결과 파싱: shape [1, numChannels, numBoxes]
            for (i in 0 until numBoxes) {
                var maxConf = 0f
                var maxClassIndex = -1

                // 각 박스에 대해 모든 클래스의 confidence 확인
                for (c in 0 until numClasses) {
                    val conf = resultData.get((4 + c) * numBoxes + i)
                    if (conf > maxConf) {
                        maxConf = conf
                        maxClassIndex = c
                    }
                }

                if (maxConf > CONFIDENCE_THRESHOLD) {
                    // cx, cy, w, h 추출
                    val cx = resultData.get(0 * numBoxes + i)
                    val cy = resultData.get(1 * numBoxes + i)
                    val w = resultData.get(2 * numBoxes + i)
                    val h = resultData.get(3 * numBoxes + i)

                    // Letterbox 좌표 → 원본 이미지 좌표로 역변환
                    val x1 = ((cx - w / 2) - padW) / scale
                    val y1 = ((cy - h / 2) - padH) / scale
                    val x2 = ((cx + w / 2) - padW) / scale
                    val y2 = ((cy + h / 2) - padH) / scale

                    if (maxClassIndex in classNames.indices) {
                        detections.add(
                            Detection(
                                className = classNames[maxClassIndex],
                                confidence = maxConf,
                                x1 = max(0f, x1),
                                y1 = max(0f, y1),
                                x2 = min(argbBitmap.width.toFloat(), x2),
                                y2 = min(argbBitmap.height.toFloat(), y2)
                            )
                        )
                    }
                }
            }
            tensor.close()
            output.close()

            Log.d(TAG, "Detected ${detections.size} objects (before NMS)")

            // 5. NMS (Non-Maximum Suppression)
            val nmsResults = applyNMS(detections, NMS_IOU_THRESHOLD)
            Log.d(TAG, "After NMS: ${nmsResults.size} objects")
            return nmsResults

        } catch (e: Exception) {
            Log.e(TAG, "Inference error", e)
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

        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }
}
