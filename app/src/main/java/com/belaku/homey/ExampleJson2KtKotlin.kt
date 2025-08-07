package com.example.example

import com.google.gson.annotations.SerializedName


data class ExampleJson2KtKotlin (

  @SerializedName("cursor" ) var cursor : Cursor? = Cursor(),
  @SerializedName("result" ) var result : Result? = Result()

)