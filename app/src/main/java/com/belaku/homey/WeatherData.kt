package com.belaku.homey

data class WeatherData(
    val name: String,
    val main: Main,
    val weather: List<Weather>
)

data class Main(
    val temp: Double
)

data class Weather(
    val main: String,
    val description: String
)