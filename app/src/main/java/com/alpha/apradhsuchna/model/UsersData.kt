package com.alpha.apradhsuchna.model

import com.google.android.gms.common.annotation.KeepName
import com.google.errorprone.annotations.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class UsersData(
    @SerializedName("name") var name:String="",
    @SerializedName("phone") var phone:String="",
    @SerializedName("role") var role:String="",
    @SerializedName("token") var token:String="")
