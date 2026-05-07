package com.eventspot.app.utilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class NotificationPermissionHelper(
    private val context: Context) {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun wasNotificationPromptShown(): Boolean {
        return prefs.getBoolean("notification_prompt_shown", false)
    }

    fun markNotificationPromptAsShown() {
        prefs.edit().putBoolean("notification_prompt_shown", true).apply()
    }

    fun syncPreferencesWithSystemPermission() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val permissionGranted = hasNotificationPermission()

        val updates = if (permissionGranted) {
            mapOf("notificationPreferences.enabled" to true)
        } else {
            mapOf(
                "notificationPreferences.enabled" to false,
                "notificationPreferences.joinedEventUpdates" to false,
                "notificationPreferences.dailyNewEvents" to false
            )
        }

        Firebase.firestore.collection("users")
            .document(userId)
            .update(updates)
    }
}
