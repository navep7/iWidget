package com.belaku.homey

data class Habit(
    val name: String,
    var isChecked: Boolean,
    var streak: Int = 0,
    var lastUpdatedDate: String = "" // Format: yyyy-MM-dd
)
