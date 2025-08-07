package com.example.example

import com.google.gson.annotations.SerializedName


data class TweetsNP (

  @SerializedName("id"        ) var id        : String? = null,
  @SerializedName("text"      ) var text      : String? = null,
  @SerializedName("likes"     ) var likes     : Int?    = null,
  @SerializedName("replies"   ) var replies   : Int?    = null,
  @SerializedName("retweets"  ) var retweets  : Int?    = null,
  @SerializedName("quotes"    ) var quotes    : Int?    = null,
  @SerializedName("timestamp" ) var timestamp : String? = null,
  @SerializedName("url"       ) var url       : String? = null

)