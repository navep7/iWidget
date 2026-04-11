package com.belaku.homey

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface ApiInterface {

    @Headers("X-RapidAPI-Host: deezerdevs-deezer.p.rapidapi.com",
                "X-RapidAPI-Key: 9e92cc4f67msh8bb4ede93f53bf7p1ecb22jsn26ea5014a6df")
    @GET("search")
    fun getDate(@Query("q") query: String) : Call<MusicData>
}