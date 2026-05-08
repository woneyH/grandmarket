package com.pbl.grandmarket_android

import android.os.Build

object KakaoMapSupport {
    val isSupportedAbi: Boolean
        get() = Build.SUPPORTED_ABIS.any { abi ->
            abi.contains("arm64") || abi.contains("armeabi")
        }
}
