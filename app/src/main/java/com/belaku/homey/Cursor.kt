package com.example.example

import com.google.gson.annotations.SerializedName


data class Cursor (

  @SerializedName("bottom" ) var bottom : String? = null,
  @SerializedName("top"    ) var top    : String? = null

)