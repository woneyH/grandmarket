package com.pbl.grandmarket_android.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiProductService {
    @GET("/api/price/average")
    suspend fun getAveragePrice(
        @Query("name") name: String
    ): Response<Long>
}
