package com.eventspot.app.repository

import com.eventspot.app.model.Event
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

        val now = System.currentTimeMillis()

        val dummyEvents = listOf(
            Event(
                id = "0",
                name = "Live Jazz Night",
                producer = "Bar Rubina",
                address = "Bar Rubina, Tel Aviv",
                description = "An intimate live jazz evening with local musicians.",
                categories = listOf("Music", "Nightlife"),
                lat = 32.07161986700242,
                lng = 34.77975831951985,
                imageUri = "https://static.wixstatic.com/media/740c76_373359e8e9f149a5a020c37c10fdb49b~mv2.jpg/v1/fill/w_640,h_422,al_c,q_80,usm_0.66_1.00_0.01,enc_avif,quality_auto/740c76_373359e8e9f149a5a020c37c10fdb49b~mv2.jpg",
                dateTimeMillis = now,
                maxParticipants = -1
            ),
            Event(
                id = "0",
                name = "Food Festival",
                producer = "Sarona Market",
                address = "Sarona Market, Tel Aviv",
                description = "Street food festival with top Israeli chefs.",
                categories = listOf("Food", "Festival"),
                lat = 32.07162975400295,
                lng = 34.787168920911284,
                imageUri = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/19/fb/18/d0/sarona-market.jpg?w=1200&h=-1&s=1",
                dateTimeMillis = now,
                maxParticipants = -1
            ),
            Event(
                id = "0",
                name = "Sunset Yoga",
                producer = "Tel Aviv Port",
                address = "Tel Aviv Port, Tel Aviv",
                description = "Outdoor sunset yoga session by the sea.",
                categories = listOf("Sport", "Wellness"),
                lat = 32.09863488892995,
                lng = 34.77361344848735,
                imageUri = "https://images.stockcake.com/public/7/e/c/7eccaeb1-9d8d-4b1f-a08e-0d9cea534390_large/sunset-yoga-meditation-stockcake.jpg",
                dateTimeMillis = now,
                maxParticipants = 1
            ),
            Event(
                id = "0",
                name = "Open Air Movie Night",
                producer = "Azrieli Center",
                address = "Azrieli Center, Tel Aviv",
                description = "Outdoor movie screening on the rooftop with city skyline views.",
                categories = listOf("Cinema", "Nightlife"),
                lat = 32.07424051550594,
                lng = 34.792202797252116,
                imageUri = "https://static.vecteezy.com/system/resources/thumbnails/074/178/701/small/a-full-red-and-white-striped-container-filled-with-fluffy-popcorn-photo.jpg",
                dateTimeMillis = now,
                maxParticipants = 30
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
            .get()
            .await()

        return snapshot.toObjects(Event::class.java)
    }

    fun observeEvents(onEventsChanged: (List<Event>) -> Unit): ListenerRegistration {
        return eventsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val events = snapshot.toObjects(Event::class.java)
                onEventsChanged(events)
            }
        }
    }
}