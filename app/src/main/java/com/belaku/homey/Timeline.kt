package com.example.example

import com.google.gson.annotations.SerializedName


data class Timeline (

  @SerializedName("instructions" ) var instructions : ArrayList<Instructions> = arrayListOf(),
  @SerializedName("metadata"     ) var metadata     : Metadata?               = Metadata()

)