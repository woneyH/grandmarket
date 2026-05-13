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

    // 28 classes
    val classNames = arrayOf(
        "아보카도", "콩", "비트", "피망", "브로콜리", "방울양배추",
        "배추", "당근", "콜리플라워", "샐러리", "옥수수", "오이",
        "가지", "강낭콩", "마늘", "매운고추", "양파", "완두콩",
        "감자", "호박", "래디시", "무", "상추", "패티슨호박",
        "토마토", "애호박", "두부", "생선"
    )

    init {
        try {
            val modelBytes = context.assets.open("expanded_28classes.onnx").readBytes()
            val options = OrtSession.SessionOptions()
            session = env.createSession(modelBytes, options)
            Log.d("YoloDetector", "ONNX Model loaded successfully")
        } catch (e: Exception) {
            Log.e("YoloDetector", "Failed to load ONNX model", e)
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

        // 1. Resize and pad image to 640x640 (Letterbox)
        val inputSize = 640
        val scale = min(inputSize.toFloat() / bitmap.width, inputSize.toFloat() / bitmap.height)
        val newW = Math.round(bitmap.width * scale)
        val newH = Math.round(bitmap.height * scale)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true)

        val padW = (inputSize - newW) / 2
        val padH = (inputSize - newH) / 2

        // 2. Convert to FloatBuffer [1, 3, 640, 640]
        val floatBuffer = FloatBuffer.allocate(3 * inputSize * inputSize)
        val pixels = IntArray(newW * newH)
        scaledBitmap.getPixels(pixels, 0, newW, 0, 0, newW, newH)

        for (i in 0 until 3 * inputSize * inputSize) {
            floatBuffer.put(i, 0.5f) // Initialize with padding color (gray/0.5)
        }

        var pixelIndex = 0
        for (y in 0 until newH) {
            for (x in 0 until newW) {
                val p = pixels[pixelIndex++]
                val r = ((p shr 16) and 0xFF) / 255.0f
                val g = ((p shr 8) and 0xFF) / 255.0f
                val b = (p and 0xFF) / 255.0f

                val destY = y + padH
                val destX = x + padW

                floatBuffer.put(0 * inputSize * inputSize + destY * inputSize + destX, r)
                floatBuffer.put(1 * inputSize * inputSize + destY * inputSize + destX, g)
                floatBuffer.put(2 * inputSize * inputSize + destY * inputSize + destX, b)
            }
        }
        floatBuffer.rewind()

        // 3. Inference
        val inputName = s.inputNames.iterator().next()
        val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
        
        try {
            val tensor = OnnxTensor.createTensor(env, floatBuffer, shape)
            val output = s.run(Collections.singletonMap(inputName, tensor))
            val resultTensor = output[0] as OnnxTensor
            val resultData = resultTensor.floatBuffer
            
            // Output shape is [1, 32, 8400]
            val numBoxes = 8400
            val numClasses = 28
            val detections = mutableListOf<Detection>()

            // 4. Parse results
            for (i in 0 until numBoxes) {
                var maxConf = 0f
                var maxClassIndex = -1

                for (c in 0 until numClasses) {
                    val conf = resultData.get(0 * 32 * numBoxes + (4 + c) * numBoxes + i)
                    if (conf > maxConf) {
                        maxConf = conf
                        maxClassIndex = c
                    }
                }

                if (maxConf > 0.5f) { // Confidence threshold
                    val cx = resultData.get(0 * 32 * numBoxes + 0 * numBoxes + i)
                    val cy = resultData.get(0 * 32 * numBoxes + 1 * numBoxes + i)
                    val w = resultData.get(0 * 32 * numBoxes + 2 * numBoxes + i)
                    val h = resultData.get(0 * 32 * numBoxes + 3 * numBoxes + i)

                    // Convert letterbox coords back to original image coords
                    val x1 = ((cx - w / 2) - padW) / scale
                    val y1 = ((cy - h / 2) - padH) / scale
                    val x2 = ((cx + w / 2) - padW) / scale
                    val y2 = ((cy + h / 2) - padH) / scale

                    detections.add(
                        Detection(
                            className = classNames[maxClassIndex],
                            confidence = maxConf,
                            x1 = max(0f, x1),
                            y1 = max(0f, y1),
                            x2 = min(bitmap.width.toFloat(), x2),
                            y2 = min(bitmap.height.toFloat(), y2)
                        )
                    )
                }
            }
            tensor.close()
            output.close()

            // 5. NMS (Non-Maximum Suppression)
            return applyNMS(detections, 0.45f)

        } catch (e: Exception) {
            Log.e("YoloDetector", "Inference error", e)
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
