package com.pbl.grandmarket_android.data.model

data class StoreLocation (
    val kakaoId: Long? = null,
    val nickname: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val viewCount: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)