package com.pbl.grandmarket_android

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.pbl.grandmarket_android.BuildConfig.KAKAO_NATIVE_APP_KEY

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, KAKAO_NATIVE_APP_KEY)
    }
}