package com.belaku

import retrofit2.http.GET
import retrofit2.http.Query

// Response object mapping Finnhub's stock quote fields
data class StockQuote(
    val c: Double,  // Current price
    val d: Double,  // Change
    val dp: Double, // Percent change
    val h: Double,  // High price of the day
    val l: Double,  // Low price of the day
    val o: Double,  // Open price of the day
    val pc: Double  // Previous close price
)

interface FinnhubApiService {
    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): StockQuote
}
