package com.example.example

import com.google.gson.annotations.SerializedName


data class Result (

  @SerializedName("timeline" ) var timeline : Timeline? = Timeline()

)