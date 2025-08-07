package com.belaku.homey

import retrofit2.Retrofit
import com.belaku.homey.ApiInterface
import retrofit2.converter.gson.GsonConverterFactory

class ApiUtilities {





    companion object {
        fun getApiInterface(): ApiInterface? {
            var retrofit: Retrofit? = null
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(ApiInterface.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit?.create(ApiInterface::class.java)
        }
    }
}