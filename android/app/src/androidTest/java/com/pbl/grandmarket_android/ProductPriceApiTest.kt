package com.pbl.grandmarket_android

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pbl.grandmarket_android.network.ApiProductService
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(AndroidJUnit4::class)
class ProductPriceApiTest {

    private val api = Retrofit.Builder()
        .baseUrl("http://192.168.0.19:8080")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiProductService::class.java)

    @Test
    fun getAveragePriceTest() {
        runBlocking {
            val response = api.getAveragePrice("배추")
            Log.d("식자재 요청 api ", "요청 시작")
            assertTrue(response.isSuccessful)

            Log.d("식자재 요청 api", "code=${response.code()} ,  body= ${response.body()}")
        }
    }

}