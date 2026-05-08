package com.pbl.grandmarket_android.repository

import com.google.firebase.Timestamp

data class StoreLocation (
    val kakaoId: Long? = null,
    val nickname: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)