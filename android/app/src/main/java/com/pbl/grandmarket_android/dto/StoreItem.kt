package com.pbl.grandmarket_android.dto

data class StoreItem(
    val storeId: String,
    val storeName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    var distanceToMe: Float = 0f // 미터(m) 단위 거리
) {
    fun getFormattedDistance(): String {
        return if (distanceToMe >= 1000) {
            String.format("%.1fkm", distanceToMe / 1000f)
        } else {
            String.format("%.0fm", distanceToMe)
        }
    }
}