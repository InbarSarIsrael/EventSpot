package com.eventspot.app.model

enum class UserRole {
    EVENT_EXPLORER,
    PRODUCER
}

data class User(
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.EVENT_EXPLORER,
    val userId: String = ""
)