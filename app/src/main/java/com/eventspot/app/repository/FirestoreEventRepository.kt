package com.eventspot.app.repository

import com.eventspot.app.model.Event
import com.eventspot.app.model.EventSource
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
class FirestoreEventRepository {

    private val db = Firebase.firestore
    private val eventsCollection = db.collection("events")

    suspend fun seedDummyEventsIfNeeded() {
        val snapshot = eventsCollection
            .limit(1)
            .get()
            .await()

        if (!snapshot.isEmpty) return

        fun millisOf(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int = 0
        ): Long {
            return java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, year)
                set(java.util.Calendar.MONTH, month - 1)
                set(java.util.Calendar.DAY_OF_MONTH, day)
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val now = System.currentTimeMillis()

        val dummyEvents = listOf(
            Event(
                id = "0",
                producerId = "dummy_producer",
                imageUri = "https://static.wixstatic.com/media/740c76_373359e8e9f149a5a020c37c10fdb49b~mv2.jpg/v1/fill/w_640,h_422,al_c,q_80/740c76_373359e8e9f149a5a020c37c10fdb49b~mv2.jpg",
                name = "Live Jazz Night",
                producer = "דמה",
                dateTimeMillis = millisOf(2026, 12, 12, 20, 0),
                endTimeMillis = millisOf(2026, 12, 12, 23, 0),
                address = "Bar Rubina, Tel Aviv",
                description = "An intimate live jazz evening with local musicians.",
                categories = listOf("Music", "Nightlife"),
                lat = 32.07161986700242,
                lng = 34.77975831951985,
                source = EventSource.PRODUCER,
                maxParticipants = -1,
                participants = emptyList(),
                externalId = "",
                sourceUrl = "",
                isActive = true,
                createdAt = now,
                updatedAt = now
            ),
            Event(
                id = "0",
                producerId = "dummy_producer",
                imageUri = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/19/fb/18/d0/sarona-market.jpg?w=1200&h=-1&s=1",
                name = "Food Festival",
                producer = "דמה",
                dateTimeMillis = millisOf(2026, 6, 12, 12, 0),
                endTimeMillis = millisOf(2026, 6, 27, 15, 0),
                address = "Sarona Market, Tel Aviv",
                description = "Street food festival with top Israeli chefs.",
                categories = listOf("Food", "Festival"),
                lat = 32.07162975400295,
                lng = 34.787168920911284,
                source = EventSource.PRODUCER,
                maxParticipants = -1,
                participants = emptyList(),
                externalId = "",
                sourceUrl = "",
                isActive = true,
                createdAt = now,
                updatedAt = now
            ),
            Event(
                id = "0",
                producerId = "dummy_producer",
                imageUri = "https://images.stockcake.com/public/7/e/c/7eccaeb1-9d8d-4b1f-a08e-0d9cea534390_large/sunset-yoga-meditation-stockcake.jpg",
                name = "Sunset Yoga",
                producer = "דמה",
                dateTimeMillis = millisOf(2026, 12, 20, 18, 0),
                endTimeMillis = millisOf(2026, 12, 20, 19, 30),
                address = "Tel Aviv Port, Tel Aviv",
                description = "Outdoor sunset yoga session by the sea.",
                categories = listOf("Sport", "Wellness"),
                lat = 32.09863488892995,
                lng = 34.77361344848735,
                source = EventSource.PRODUCER,
                maxParticipants = 1,
                participants = emptyList(),
                externalId = "",
                sourceUrl = "",
                isActive = true,
                createdAt = now,
                updatedAt = now
            ),
            Event(
                id = "0",
                producerId = "dummy_producer",
                imageUri = "https://static.vecteezy.com/system/resources/thumbnails/074/178/701/small/a-full-red-and-white-striped-container-filled-with-fluffy-popcorn-photo.jpg",
                name = "Open Air Movie Night",
                producer = "דמה",
                dateTimeMillis = millisOf(2026, 12, 31, 21, 0),
                endTimeMillis = millisOf(2027, 1, 1, 0, 30),
                address = "Azrieli Center, Tel Aviv",
                description = "Outdoor movie screening on the rooftop with city skyline views.",
                categories = listOf("Cinema", "Nightlife"),
                lat = 32.07424051550594,
                lng = 34.792202797252116,
                source = EventSource.PRODUCER,
                maxParticipants = 30,
                participants = emptyList(),
                externalId = "",
                sourceUrl = "",
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
        )

        val batch = db.batch()

        dummyEvents.forEach { event ->
            val docRef = eventsCollection.document()
            val eventWithId = event.copy(id = docRef.id)
            batch.set(docRef, eventWithId)
        }

        batch.commit().await()
    }

    suspend fun getAllEvents(): List<Event> {
        val snapshot = eventsCollection
            .whereEqualTo("isActive", true)
            .get()
            .await()

        val events = snapshot.toObjects(Event::class.java)
        return filterVisibleEvents(events)
    }

//    fun observeEvents(onEventsChanged: (List<Event>) -> Unit): ListenerRegistration {
//        return eventsCollection
//            .whereEqualTo("isActive", true)
//            .addSnapshotListener { snapshot, error ->
//                if (error != null) {
//                    error.printStackTrace()
//                    return@addSnapshotListener
//                }
//
//                if (snapshot != null) {
//                    val events = snapshot.toObjects(Event::class.java)
//                    onEventsChanged(filterVisibleEvents(events))
//                }
//            }
//    }

    fun observeEvents(onEventsChanged: (List<Event>) -> Unit): ListenerRegistration {
        return eventsCollection
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val events = snapshot.toObjects(Event::class.java)
                    println("DEBUG events from firestore: ${events.size}")

                    events.forEach {
                        println(
                            "DEBUG event: ${it.name}, start=${it.dateTimeMillis}, end=${it.endTimeMillis}, active=${it.isActive}"
                        )
                    }

                    onEventsChanged(events)
                }
            }
    }

    private fun filterVisibleEvents(events: List<Event>): List<Event> {
        val now = System.currentTimeMillis()

        return events.filter { event ->
            val eventEndMillis = getEventEndMillis(event)

            event.isActive && eventEndMillis >= now
        }
    }

    private fun getEventEndMillis(event: Event): Long {
        return if (event.endTimeMillis > 0L) {
            event.endTimeMillis
        } else {
            event.dateTimeMillis
        }
    }
}