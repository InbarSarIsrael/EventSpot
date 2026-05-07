package com.eventspot.app.utilities

import com.eventspot.app.repository.FirestoreUserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EventSpotMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val userRepository = FirestoreUserRepository()

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        serviceScope.launch {
            userRepository.saveFcmToken(userId, token)
        }
    }
}
