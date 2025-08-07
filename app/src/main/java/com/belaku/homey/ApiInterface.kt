package com.belaku.homey

import org.intellij.lang.annotations.Language
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call


interface ApiInterface {



    @GET("everything")
    fun getNews(
        @Query("q") q: String,
        @Query("from") from: String,
        @Query("sortBy") sortBy: String,
        @Query("language") language: String,
      //  @Query("pageSize") pagesize: Int,
        @Query("apikey") apikey: String
    ): Call<MainNews>


    @GET("top-headlines")
    fun getCategory(
        @Query("country") country: String,
        @Query("category") category: String,
        @Query("pageSize") pagesize: String,
        @Query("apikey") apikey: String
    ): Call<MainNews>

    companion object {
        val BASE_URL: String
             = "https://newsapi.org/v2/"
    }
}