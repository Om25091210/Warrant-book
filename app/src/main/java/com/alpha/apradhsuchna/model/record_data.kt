package com.alpha.apradhsuchna.model

import com.google.errorprone.annotations.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class record_data(
    @SerializedName("address") var address: String = "",
    @SerializedName("age")var age: String = "",
    @SerializedName("co")var co: String = "",
    @SerializedName("case_no") var case_no: String = "",
    @SerializedName("court_name") var court_name: String = "",
    @SerializedName("done") var done: String = "",
    @SerializedName("criminal_name")  var criminal_name: String = "",
    @SerializedName("image_link") var image_link: String = "",
    @SerializedName("more_details") var more_details: String = "",
    @SerializedName("police_station") var police_station: String = "",
    @SerializedName("section") var section: String = "",
    @SerializedName("uid") var uid: String = "",
    @SerializedName("uploaded_date") var uploaded_date: String = "",
    @SerializedName("key") var key: String = ""
)
