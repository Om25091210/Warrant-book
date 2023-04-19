package com.alpha.apradhsuchna.model

import com.google.android.gms.common.annotation.KeepName
import com.google.errorprone.annotations.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Comments(
    @SerializedName("name") var name: String = "",
    @SerializedName("time") var time:String = "",
    @SerializedName("content") var content:String ="",
    @SerializedName("uid") var uid:String=""
)
