package com.eventspot.app.model

enum class UserRole {
    EVENT_EXPLORER,
    PRODUCER
}

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: UserRole = UserRole.EVENT_EXPLORER
)