package com.alpha.apradhsuchna.fcm

import com.alpha.apradhsuchna.fcm.NotificationData

data class PushNotification (
    val data: NotificationData,
    val to:String
)