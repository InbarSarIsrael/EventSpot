package com.eventspot.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.eventspot.app.databinding.ActivitySettingsBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var isUpdatingUi = false
    private var notificationsEnabledPreference = false
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                saveNotificationPreference("enabled", true)
                notificationsEnabledPreference = true
            } else {
                saveNotificationPreference("enabled", false)
                notificationsEnabledPreference = false
                Toast.makeText(this, "Notifications permission is required", Toast.LENGTH_SHORT).show()
            }

            updateNotificationPermissionStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
        setupNotificationSwitches()
        loadNotificationSettingsFromFirestore()
        updateLocationStatus()
    }

    override fun onResume() {
        super.onResume()
        updateLocationStatus()
        loadNotificationSettingsFromFirestore()
    }

    private fun loadNotificationSettingsFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        Firebase.firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val prefs = document.get("notificationPreferences") as? Map<*, *>
                val notificationPermissionGranted = hasNotificationPermission()

                val enabled = prefs?.get("enabled") as? Boolean ?: false
                val joinedUpdates = prefs?.get("joinedEventUpdates") as? Boolean ?: false
                val dailyNewEvents = prefs?.get("dailyNewEvents") as? Boolean ?: false
                notificationsEnabledPreference = enabled || notificationPermissionGranted

                isUpdatingUi = true
                binding.settingsSWITCHJoinedEventUpdates.isChecked =
                    joinedUpdates && notificationPermissionGranted
                binding.settingsSWITCHDailyNewEvents.isChecked =
                    dailyNewEvents && notificationPermissionGranted
                isUpdatingUi = false

                when {
                    !notificationPermissionGranted -> clearNotificationPreferences()
                    !enabled -> saveNotificationPreference("enabled", true)
                }

                updateNotificationPermissionStatus()
            }
    }

    private fun setupNotificationSwitches() {
        binding.settingsSWITCHJoinedEventUpdates.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingUi) return@setOnCheckedChangeListener
            if (!canChooseNotificationTypes()) {
                isUpdatingUi = true
                binding.settingsSWITCHJoinedEventUpdates.isChecked = false
                isUpdatingUi = false
                openAppSettings()
                return@setOnCheckedChangeListener
            }

            saveNotificationPreference("joinedEventUpdates", isChecked)
        }

        binding.settingsSWITCHDailyNewEvents.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingUi) return@setOnCheckedChangeListener
            if (!canChooseNotificationTypes()) {
                isUpdatingUi = true
                binding.settingsSWITCHDailyNewEvents.isChecked = false
                isUpdatingUi = false
                openAppSettings()
                return@setOnCheckedChangeListener
            }

            saveNotificationPreference("dailyNewEvents", isChecked)
        }
    }

    private fun updateNotificationDependentSwitches(enabled: Boolean) {
        binding.settingsSWITCHJoinedEventUpdates.isEnabled = enabled
        binding.settingsSWITCHDailyNewEvents.isEnabled = enabled

        val alpha = if (enabled) 1f else 0.5f
        binding.settingsSWITCHJoinedEventUpdates.alpha = alpha
        binding.settingsSWITCHDailyNewEvents.alpha = alpha
    }

    private fun canChooseNotificationTypes(): Boolean {
        return hasNotificationPermission()
    }

    private fun updateNotificationPermissionStatus() {
        val notificationsEnabled = hasNotificationPermission()

        binding.settingsLBLNotificationStatus.text = if (notificationsEnabled) {
            "Status: Allowed"
        } else {
            "Status: Not allowed"
        }

        updateNotificationDependentSwitches(notificationsEnabled)
    }

    private fun setupButtons() {
        binding.settingsBTNOpenAppSettings.setOnClickListener {
            openAppSettings()
        }

        binding.settingsBTNManageNotifications.setOnClickListener {
            manageNotificationPermission()
        }
    }

    private fun manageNotificationPermission() {
        if (!hasNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openAppSettings()
        }
    }

    private fun updateLocationStatus() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        binding.settingsLBLLocationStatus.text = when {
            fineLocationGranted -> "Status: Allowed"
            coarseLocationGranted -> "Status: Allowed"
            else -> "Status: Not allowed"
        }
    }

    private fun saveNotificationPreference(field: String, value: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        Firebase.firestore.collection("users")
            .document(userId)
            .update("notificationPreferences.$field", value)
    }

    private fun clearNotificationPreferences() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        notificationsEnabledPreference = false

        Firebase.firestore.collection("users")
            .document(userId)
            .update(
                mapOf(
                    "notificationPreferences.enabled" to false,
                    "notificationPreferences.joinedEventUpdates" to false,
                    "notificationPreferences.dailyNewEvents" to false
                )
            )
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }
}
