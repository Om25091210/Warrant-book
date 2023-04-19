package com.alpha.apradhsuchna.fcm

import com.alpha.apradhsuchna.fcm.Constants.Companion.CONTENT_TYPE
import com.alpha.apradhsuchna.fcm.Constants.Companion.SERVER_KEY
import okhttp3.ResponseBody
import retrofit2.Response


import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
interface NotificationAPI {

    @Headers("Authorization: key=$SERVER_KEY","Content_Type:$CONTENT_TYPE")
    @POST("fcm/send")
    suspend fun postNotification(
        @Body notification: PushNotification
    ): Response<ResponseBody>
}