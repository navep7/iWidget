package com.belaku.homey

import com.google.gson.annotations.SerializedName

data class WeatherData(
    @SerializedName("current_weather")
    val currentWeather: CurrentWeather
)

data class CurrentWeather(
    val temperature: Double,
    val weathercode: Int
)
