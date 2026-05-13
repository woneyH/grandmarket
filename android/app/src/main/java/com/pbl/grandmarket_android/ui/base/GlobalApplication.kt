package com.pbl.grandmarket_android.ui.base

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk
import com.pbl.grandmarket_android.BuildConfig
import com.pbl.grandmarket_android.util.KakaoMapSupport

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        if (KakaoMapSupport.isSupportedAbi) {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
    }
}