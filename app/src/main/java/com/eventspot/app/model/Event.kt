package com.eventspot.app.model

enum class EventSource {
    USER, API
}

data class Event(
    val id: String = "",                    // Unique ID of the event (used for update/delete in DB)

    val imageUri: String = "",              // URL of the event image (Firebase Storage or external API)

    val name: String = "",                  // Event title

    val producer: String = "",              // Organizer / producer name

    val dateTimeMillis: Long = 0L,          // Event date & time as timestamp

    val address: String = "",              // Event address location

    val description: String = "",           // Short event description

    val categories: List<String> = emptyList(), // List of selected categories (1–3 for user events)

    val lat: Double = 0.0,                  // Latitude coordinate (for maps & distance calculation)

    val lng: Double = 0.0,                  // Longitude coordinate (for maps & distance calculation)

    val source: EventSource = EventSource.USER, // Indicates if event came from USER or API

    val createdAt: Long = System.currentTimeMillis(), // Creation timestamp (used for "new events" sorting)

    val maxParticipants: Int = -1, // Max people in event

    val participants: List<String> = emptyList(), // List of participants
)



