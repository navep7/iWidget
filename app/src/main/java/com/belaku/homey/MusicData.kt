package com.belaku.homey

import com.belaku.Data

data class MusicData(
    val `data`: List<Data>,
    val next: String,
    val total: Int
)