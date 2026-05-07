package com.eventspot.app.model

import com.google.firebase.firestore.PropertyName

enum class EventSource {
    PRODUCER, TEL_AVIV_MUNICIPALITY
}

data class Event(
    val id: String = "",                    // Unique ID of the event (used for update/delete in DB)

    val producerId: String = "",            // ID of the event producer (userId or external source identifier)

    val imageUri: String = "",              // URL of the event image (Firebase Storage or external API)

    val name: String = "",                  // Event title

    val producer: String = "",              // Organizer / producer name (user name or external provider)

    val dateTimeMillis: Long = 0L,          // Event start timestamp

    val address: String = "",              // Event address location

    val description: String = "",           // Short event description

    val categories: List<String> = emptyList(), // List of selected categories (1–3 for user events)

    val lat: Double = 0.0,                  // Latitude coordinate (for maps & distance calculation)

    val lng: Double = 0.0,                  // Longitude coordinate (for maps & distance calculation)

    val source: EventSource = EventSource.PRODUCER, // Indicates if event came from USER or imported from Tel Aviv Municipality

    val maxParticipants: Int = -1, // Max people in event, relevant for user-created events only

    val participants: List<String> = emptyList(), // Registered users (user events only)

    val externalId: String = "",        // Unique ID from the external source

    val sourceUrl: String = "",         // URL of the original event page on the source website

    val endTimeMillis: Long = 0L,       // Event end timestamp

    @get:PropertyName("isActive")
    val isActive: Boolean = true,       // Indicates whether the event is currently active (soft delete control)

    val createdAt: Long = System.currentTimeMillis(), // Creation timestamp (used for "new events" sorting)

    val updatedAt: Long = System.currentTimeMillis() // Last update timestamp
)