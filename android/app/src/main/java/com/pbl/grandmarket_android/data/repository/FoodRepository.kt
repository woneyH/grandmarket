package com.pbl.grandmarket_android.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.kakao.sdk.user.UserApiClient
import com.pbl.grandmarket_android.data.model.FoodEntity
import java.io.ByteArrayOutputStream

data class SellerFoodItem(
    val id: String,
    val foodName: String,
    val price: Long,
    val category: String,
    val status: String,
    val timestamp: Long,
    val sellerName: String,
    val imageUrl: String = ""
)

class FoodRepository {
    private val db = FirebaseFirestore.getInstance()

    // 점포 등록 여부만 확인하는 전용 함수 (AI 인식 후 등록 유도에 사용)
    fun checkStoreExists(onResult: (isExists: Boolean, message: String?) -> Unit) {
        UserApiClient.instance.me { user, error ->
            if (error != null || user == null) {
                onResult(false, "사용자 정보를 불러오지 못했습니다")
                return@me
            }

            val kakaoId = user.id.toString()
            db.collection("storeLocation").document(kakaoId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        onResult(true, null)
                    } else {
                        onResult(false, "점포를 먼저 등록해주세요")
                    }
                }
                .addOnFailureListener { error ->
                    Log.d("FoodRepository", "점포 확인 에러 (네트워크)")
                    onResult(false, "네트워크 오류 발생")
                }
        }
    }

    fun foodAddWithValidation(
        foodName: String,
        price: Long,
        marketPrice: Long,
        imageUri: Uri? = null,
        onResult: (isSuccess: Boolean, message: String?) -> Unit
    ) {
        UserApiClient.instance.me { user, error ->
            if (error != null || user == null) {
                return@me
            }

            val kakaoId = user.id.toString()
            val nickname = user.kakaoAccount?.profile?.nickname ?: "익명 판매자"

            // 점포 등록된 상태인지 확인
            db.collection("storeLocation").document(kakaoId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        if (imageUri != null) {
                            uploadImageThenSave(kakaoId, nickname, foodName, price, marketPrice, imageUri, onResult)
                        } else {
                            saveFoodToFirebase(kakaoId, nickname, foodName, price, marketPrice, "", onResult)
                        }
                    } else {
                        // TODO: "점포를 먼저 등록해주세요" 팝업이나 알림 띄우기
                        onResult(false, "점포를 먼저 등록해주세요")
                    }
                }
                .addOnFailureListener { error ->
                    Log.d("FoodRepository", "점포 확인 에러 (네트워크)")
                    onResult(false,"네트워크 오류 발생")
                }
        }
    }

    private fun uploadImageThenSave(
        kakaoId: String,
        sellerName: String,
        foodName: String,
        price: Long,
        marketPrice: Long,
        imageUri: Uri,
        onResult: (Boolean, String?) -> Unit
    ) {
        val compressedBytes = compressImage(imageUri)
        if (compressedBytes == null) {
            onResult(false, "이미지 처리에 실패했습니다")
            return
        }
        Log.d("FoodRepository", "압축 후 이미지 크기: ${compressedBytes.size / 1024} KB")

        val storageRef = FirebaseStorage.getInstance().reference
            .child("food_images/$kakaoId/${System.currentTimeMillis()}.jpg")

        storageRef.putBytes(compressedBytes)
            .addOnSuccessListener {
                storageRef.downloadUrl
                    .addOnSuccessListener { downloadUrl ->
                        Log.d("FoodRepository", "이미지 업로드 성공: $downloadUrl")
                        saveFoodToFirebase(kakaoId, sellerName, foodName, price, marketPrice, downloadUrl.toString(), onResult)
                    }
                    .addOnFailureListener { e ->
                        Log.e("FoodRepository", "다운로드 URL 가져오기 실패", e)
                        onResult(false, "이미지 URL 가져오기 실패: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FoodRepository", "이미지 업로드 실패: ${e.message}", e)
                onResult(false, "이미지 업로드 실패: ${e.message}")
            }
    }
    private fun compressImage(imageUri: Uri): ByteArray? {
        val context = FirebaseApp.getInstance().applicationContext
        return try {
            // 1차 디코딩: 실제 픽셀을 읽지 않고 원본 크기만 측정
            val sizeOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it, null, sizeOptions)
            }

            // 1024px 이내로 줄일 inSampleSize 계산 (2의 거듭제곱)
            val maxPx = 1024
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calcInSampleSize(sizeOptions.outWidth, sizeOptions.outHeight, maxPx)
            }

            // 2차 디코딩: inSampleSize를 적용해 메모리 절약하며 디코딩
            val decoded = context.contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            // inSampleSize는 2의 거듭제곱이라 maxPx를 약간 초과할 수 있어 최종 스케일 조정
            val scaled = if (decoded.width > maxPx || decoded.height > maxPx) {
                val ratio = minOf(maxPx.toFloat() / decoded.width, maxPx.toFloat() / decoded.height)
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * ratio).toInt(),
                    (decoded.height * ratio).toInt(),
                    true
                ).also { if (it !== decoded) decoded.recycle() }
            } else decoded

            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
                scaled.recycle()
                out.toByteArray()
            }
        } catch (e: Exception) {
            Log.e("FoodRepository", "이미지 압축 실패", e)
            null
        }
    }

    private fun calcInSampleSize(width: Int, height: Int, maxPx: Int): Int {
        var sample = 1
        while ((width / sample) > maxPx || (height / sample) > maxPx) {
            sample *= 2
        }
        return sample
    }

    private fun saveFoodToFirebase(
        kakaoId: String,
        sellerName: String,
        foodName: String,
        price: Long,
        marketPrice: Long,
        imageUrl: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val foodData = FoodEntity(
            kakaoId = kakaoId,
            sellerName = sellerName,
            foodName = foodName,
            price = price,
            marketPrice = marketPrice,
            imageUrl = imageUrl
        )

        db.collection("foodList").add(foodData)
            .addOnSuccessListener { documentReference ->
                Log.d("FoodRepository", "상품 등록 성공: ${documentReference.id}")
                onResult(true, "${foodName} 성공적으로 등록")
            }
            .addOnFailureListener { error ->
                Log.d("FoodRepository", "상품 등록 실패: ", error)
                onResult(false, "${foodName} 등록 실패!!")
            }
    }

    fun getSellerFoodList(
        onResult: (isSuccess: Boolean, items: List<SellerFoodItem>, message: String?) -> Unit
    ) {
        UserApiClient.instance.me { user, error ->
            if (error != null || user == null) {
                onResult(false, emptyList(), "사용자 정보를 불러오지 못했습니다")
                return@me
            }

            val kakaoId = user.id.toString()
            db.collection("foodList")
                .whereEqualTo("kakaoId", kakaoId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val items = querySnapshot.documents.mapNotNull { document ->
                        val food = document.toObject(FoodEntity::class.java) ?: return@mapNotNull null
                        SellerFoodItem(
                            id = document.id,
                            foodName = food.foodName,
                            price = food.price,
                            category = food.category,
                            status = food.status,
                            timestamp = food.timestamp,
                            sellerName = food.sellerName,
                            imageUrl = food.imageUrl
                        )
                    }.sortedByDescending { it.timestamp }
                    onResult(true, items, null)
                }
                .addOnFailureListener { fetchError ->
                    Log.e("FoodRepository", "판매 목록 조회 실패", fetchError)
                    onResult(false, emptyList(), "판매 목록을 불러오지 못했습니다")
                }
        }
    }

    fun updateFoodStatus(
        foodId: String,
        newStatus: String,
        onResult: (isSuccess: Boolean, message: String?) -> Unit
    ) {
        db.collection("foodList")
            .document(foodId)
            .update("status", newStatus)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { updateError ->
                Log.e("FoodRepository", "상품 상태 변경 실패", updateError)
                onResult(false, "상품 상태 변경에 실패했습니다")
            }
    }

    fun deleteFoodItem(
        foodId: String,
        onResult: (isSuccess: Boolean, message: String?) -> Unit
    ) {
        db.collection("foodList")
            .document(foodId)
            .delete()
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { deleteError ->
                Log.e("FoodRepository", "상품 삭제 실패", deleteError)
                onResult(false, "상품 삭제에 실패했습니다")
            }
    }

    // 판매자 통계 조회 - 총 판매 수, 판매완료 수, 점포 조회수를 Firebase에서 가져옴
    fun getSellerStats(
        onResult: (totalCount: Int, soldCount: Int, viewCount: Int) -> Unit
    ) {
        UserApiClient.instance.me { user, error ->
            if (error != null || user == null) {
                onResult(0, 0, 0)
                return@me
            }
            val kakaoId = user.id.toString()

            db.collection("foodList")
                .whereEqualTo("kakaoId", kakaoId)
                .get()
                .addOnSuccessListener { foodSnapshot ->
                    val totalCount = foodSnapshot.size()
                    val soldCount = foodSnapshot.documents.count { doc ->
                        doc.getString("status") == "판매완료"
                    }

                    db.collection("storeLocation").document(kakaoId).get()
                        .addOnSuccessListener { storeDoc ->
                            val viewCount = storeDoc.getLong("viewCount")?.toInt() ?: 0
                            onResult(totalCount, soldCount, viewCount)
                        }
                        .addOnFailureListener {
                            onResult(totalCount, soldCount, 0)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("FoodRepository", "판매자 통계 조회 실패", e)
                    onResult(0, 0, 0)
                }
        }
    }

    // 구매자가 점포를 조회할 때 storeLocation의 viewCount 필드를 1 증가
    fun incrementStoreViewCount(kakaoId: String) {
        db.collection("storeLocation").document(kakaoId)
            .update("viewCount", FieldValue.increment(1))
            .addOnFailureListener { e ->
                Log.e("FoodRepository", "viewCount 업데이트 실패 kakaoId=$kakaoId", e)
            }
    }

    fun getNearbyOnSaleFoodList(
        userLat: Double,
        userLng: Double,
        searchRadiusMeter: Float,
        onResult: (isSuccess: Boolean, items: List<SellerFoodItem>, message: String?) -> Unit
    ) {
        db.collection("storeLocation")
            .get()
            .addOnSuccessListener { storesSnapshot ->
                val nearbySellerIds = storesSnapshot.documents.mapNotNull { storeDoc ->
                    val storeLat = storeDoc.getDouble("latitude") ?: return@mapNotNull null
                    val storeLng = storeDoc.getDouble("longitude") ?: return@mapNotNull null
                    val distanceResult = FloatArray(1)
                    Location.distanceBetween(userLat, userLng, storeLat, storeLng, distanceResult)
                    if (distanceResult[0] <= searchRadiusMeter) {
                        storeDoc.id
                    } else {
                        null
                    }
                }.toSet()

                if (nearbySellerIds.isEmpty()) {
                    onResult(true, emptyList(), null)
                    return@addOnSuccessListener
                }

                db.collection("foodList")
                    .whereEqualTo("status", "판매중")
                    .get()
                    .addOnSuccessListener { foodsSnapshot ->
                        val items = foodsSnapshot.documents.mapNotNull { foodDoc ->
                            val food = foodDoc.toObject(FoodEntity::class.java) ?: return@mapNotNull null
                            if (!nearbySellerIds.contains(food.kakaoId)) {
                                return@mapNotNull null
                            }
                            SellerFoodItem(
                                id = foodDoc.id,
                                foodName = food.foodName,
                                price = food.price,
                                category = food.category,
                                status = food.status,
                                timestamp = food.timestamp,
                                sellerName = food.sellerName,
                                imageUrl = food.imageUrl
                            )
                        }.sortedByDescending { it.timestamp }

                        onResult(true, items, null)
                    }
                    .addOnFailureListener { foodsError ->
                        Log.e("FoodRepository", "주변 판매 상품 조회 실패", foodsError)
                        onResult(false, emptyList(), "주변 판매 상품을 불러오지 못했습니다")
                    }
            }
            .addOnFailureListener { storesError ->
                Log.e("FoodRepository", "점포 위치 조회 실패", storesError)
                onResult(false, emptyList(), "점포 위치를 불러오지 못했습니다")
            }
    }
}
