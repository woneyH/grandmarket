package com.pbl.grandmarket_android.data.model

data class FoodEntity (
    val kakaoId: String = "",   //카카오 고유 식별 id
    val sellerName: String = "",    //판매자 카카오 닉네임
    val foodName: String = "",  //식자재 이름
    val price: Long = 0L,   //판매자가 지정한 가격
    val marketPrice: Long = 0L, //해당 식자재 평균 시세
    val category: String = "",  //식자재 카테고리
    val status: String = "판매중", //판매 상황
    val timestamp: Long = System.currentTimeMillis()
)