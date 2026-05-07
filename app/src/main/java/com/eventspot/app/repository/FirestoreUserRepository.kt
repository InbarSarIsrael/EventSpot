package com.eventspot.app.repository

import com.eventspot.app.model.NotificationPreferences
import com.eventspot.app.model.User
import com.eventspot.app.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class FirestoreUserRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")



    suspend fun createUserIfNotExists(userId: String, email: String, name: String) {
        val userDoc = usersCollection.document(userId).get().await()

        if (!userDoc.exists()) {
            val user = User(
                email = email,
                name = name,
                role = null,
                userId = userId,
                notificationPreferences = NotificationPreferences()
            )

            usersCollection
                .document(userId)
                .set(user)
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

    suspend fun getUserRole(userId: String): UserRole? {
        val userDoc = usersCollection
            .document(userId)
            .get()
            .await()

        val role = userDoc.getString("role") ?: return null

        return try {
            UserRole.valueOf(role)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    suspend fun saveFcmToken(userId: String, token: String) {
        val tokenData = hashMapOf(
            "token" to token,
            "updatedAt" to System.currentTimeMillis()
        )

        usersCollection
            .document(userId)
            .collection("fcmTokens")
            .document(token.toStableDocumentId())
            .set(tokenData)
            .await()
    }

    suspend fun deleteFcmToken(userId: String, token: String) {
        usersCollection
            .document(userId)
            .collection("fcmTokens")
            .document(token.toStableDocumentId())
            .delete()
            .await()
    }

    private fun String.toStableDocumentId(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
