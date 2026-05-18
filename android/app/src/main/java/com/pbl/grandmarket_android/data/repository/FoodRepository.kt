package com.pbl.grandmarket_android.data.repository

import android.location.Location
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.sdk.user.UserApiClient
import com.pbl.grandmarket_android.data.model.FoodEntity

data class SellerFoodItem(
    val id: String,
    val foodName: String,
    val price: Long,
    val category: String,
    val status: String,
    val timestamp: Long
)

class FoodRepository {
    private val db = FirebaseFirestore.getInstance()

    fun foodAddWithValidation(
        foodName: String,
        price: Long,
        marketPrice: Long,
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
                        saveFoodToFirebase(kakaoId, nickname, foodName, price, marketPrice, onResult)
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

    private fun saveFoodToFirebase(
        kakaoId: String,
        sellerName: String,
        foodName: String,
        price: Long,
        marketPrice: Long,
        onResult: (Boolean, String?) -> Unit
    ) {
        val foodData = FoodEntity(
            kakaoId = kakaoId,
            sellerName = sellerName,
            foodName = foodName,
            price = price,
            marketPrice = marketPrice
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
                            timestamp = food.timestamp
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
                                timestamp = food.timestamp
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
