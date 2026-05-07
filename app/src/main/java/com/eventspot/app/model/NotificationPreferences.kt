package com.eventspot.app.model

data class NotificationPreferences(
    val enabled: Boolean = false,
    val joinedEventUpdates: Boolean = false,
    val dailyNewEvents: Boolean = false
)