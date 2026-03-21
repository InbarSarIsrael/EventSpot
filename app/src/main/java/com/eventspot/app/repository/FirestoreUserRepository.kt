package com.eventspot.app.repository

import com.eventspot.app.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")



    suspend fun createUserIfNotExists(userId: String, email: String, name: String) {
        val userDoc = usersCollection.document(userId).get().await()

        if (!userDoc.exists()) {
            val userData = hashMapOf(
                "userId" to userId,
                "email" to email,
                "name" to name,
                "createdAt" to System.currentTimeMillis(),
                "role" to null
            )

            usersCollection
                .document(userId)
                .set(userData)
                .await()
        }
    }

    suspend fun hasUserRole(userId: String): Boolean {
        val userDoc = usersCollection
            .document(userId)
            .get()
            .await()

        val role = userDoc.getString("role")
        return !role.isNullOrBlank() // false if null
    }

    suspend fun saveUserRole(userId: String, role: UserRole) {
        usersCollection
            .document(userId)
            .update("role", role.name)
            .await()
    }

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