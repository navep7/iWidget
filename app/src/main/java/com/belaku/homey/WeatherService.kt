package com.belaku.homey

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {

    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") lat: String,
        @Query("longitude") lon: String,
        @Query("current_weather") currentWeather: Boolean = true
    ): WeatherData
}