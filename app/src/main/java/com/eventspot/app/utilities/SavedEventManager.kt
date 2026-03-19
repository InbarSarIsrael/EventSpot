package com.eventspot.app.utilities

import com.eventspot.app.repository.FirestoreUserRepository
import com.google.firebase.auth.FirebaseAuth

class SavedEventsManager(
    private val userRepository: FirestoreUserRepository = FirestoreUserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    suspend fun isEventSaved(eventId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return userRepository.isEventSaved(userId, eventId)
    }

    suspend fun toggleSaved(eventId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false

        val isCurrentlySaved = userRepository.isEventSaved(userId, eventId)

        if (isCurrentlySaved) {
            userRepository.unsaveEvent(userId, eventId)
            return false
        } else {
            userRepository.saveEvent(userId, eventId)
            return true
        }
    }

    suspend fun getSavedEventIds(): Set<String> {
        val userId = auth.currentUser?.uid ?: return emptySet()
        return userRepository.getSavedEventIds(userId).toSet()
    }
}