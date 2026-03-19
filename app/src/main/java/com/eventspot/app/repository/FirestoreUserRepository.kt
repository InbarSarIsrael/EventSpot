package com.eventspot.app.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    suspend fun saveEvent(userId: String, eventId: String) {
        val savedEventData = hashMapOf(
            "eventId" to eventId,
            "savedAt" to System.currentTimeMillis()
        )

        usersCollection
            .document(userId)
            .collection("savedEvents")
            .document(eventId)
            .set(savedEventData)
            .await()
    }

    suspend fun unsaveEvent(userId: String, eventId: String) {
        usersCollection
            .document(userId)
            .collection("savedEvents")
            .document(eventId)
            .delete()
            .await()
    }

    suspend fun isEventSaved(userId: String, eventId: String): Boolean {
        val snapshot = usersCollection
            .document(userId)
            .collection("savedEvents")
            .document(eventId)
            .get()
            .await()

        return snapshot.exists()
    }

    suspend fun getSavedEventIds(userId: String): List<String> {
        val snapshot = usersCollection
            .document(userId)
            .collection("savedEvents")
            .get()
            .await()

        return snapshot.documents.map { it.id }
    }
}