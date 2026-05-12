package com.pbl.grandmarket_android.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.sdk.user.UserApiClient
import com.pbl.grandmarket_android.data.model.FoodEntity

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
}