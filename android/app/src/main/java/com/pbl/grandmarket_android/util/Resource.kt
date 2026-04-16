package com.pbl.grandmarket_android.util

import android.util.Log

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error<out T>(val data: T?) : Resource<Nothing>()
    object Loading : Resource<Nothing>() {
        fun callLog() {
            Log.d("loading call", "로딩 객체 호출 됨")
        }
    }
}