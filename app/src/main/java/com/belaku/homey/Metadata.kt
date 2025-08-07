package com.example.example

import com.google.gson.annotations.SerializedName


data class Metadata (

  @SerializedName("scribeConfig" ) var scribeConfig : ScribeConfig? = ScribeConfig()

)