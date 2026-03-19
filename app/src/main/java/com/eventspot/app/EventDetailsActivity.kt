package com.eventspot.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.eventspot.app.databinding.ActivityEventDetailsBinding
import com.eventspot.app.model.Event
import com.eventspot.app.repository.FirestoreUserRepository
import com.eventspot.app.utilities.SavedEventsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailsBinding
    private val db = FirebaseFirestore.getInstance()
    private val savedEventsManager = SavedEventsManager()
    private var isSaved = false

    private var currentEvent: Event? = null

    private var eventListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateSavedButton()
        setupBackButton()
        setupNavigateButton()
        setupJoinButton()
        setupSaveButton()
        loadEventData()
    }

    private fun setupBackButton() {
        binding.eventDetailsBTNBack.setOnClickListener {
            finish()
        }
    }

    private fun setupNavigateButton() {
        binding.eventDetailsBTNNavigate.setOnClickListener {
            val event = currentEvent ?: return@setOnClickListener
            openNavigation(event.lat, event.lng)
        }
    }

    private fun setupJoinButton() {
        binding.eventDetailsBTNJoin.setOnClickListener {
            val event = currentEvent ?: return@setOnClickListener
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            if (userId in event.participants) {
                cancelJoin(event, userId)
            } else {
                joinEvent(event, userId)
            }
        }
    }

    private fun setupSaveButton() {
        binding.eventDetailsBTNSaved.setOnClickListener {
            val event = currentEvent ?: return@setOnClickListener
            toggleSaveEvent(event.id)
        }
    }

    private fun loadEventData() {
        val eventId = intent.getStringExtra("event_id")

        if (eventId.isNullOrEmpty()) {
            Toast.makeText(this, "Event ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        eventListener = db.collection("events")
            .document(eventId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val event = document.toObject(Event::class.java)?.copy(id = document.id)

                    if (event != null) {
                        currentEvent = event
                        bindEventData(event)
                    } else {
                        Toast.makeText(this, "Failed to load event data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    @SuppressLint("SetTextI18n")
    private fun bindEventData(event: Event) {
        binding.eventDetailsLBLName.text = event.name
        binding.eventDetailsLBLProducer.text = "Producer: ${event.producer}"
        binding.eventDetailsLBLLocation.text = "Address: ${event.address}"
        binding.eventDetailsLBLDescription.text = event.description
        binding.eventDetailsLBLCategories.text =
            "Categories: ${event.categories.joinToString(", ")}"
        binding.eventDetailsLBLDate.text = "Date: ${formatDate(event.dateTimeMillis)}"
        binding.eventDetailsLBLTime.text = "Time: ${formatTime(event.dateTimeMillis)}"

        binding.eventDetailsBTNNavigate.visibility =
            if (event.lat == 0.0 && event.lng == 0.0) android.view.View.INVISIBLE else android.view.View.VISIBLE

        updateJoinButton(event)
        checkIfEventSaved(event.id)

        if (event.imageUri.isNotEmpty()) {
            Glide.with(this)
                .load(event.imageUri)
                .into(binding.eventDetailsIMGEvent)
        }
    }

    private fun openNavigation(lat: Double, lng: Double) {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
        val intent = Intent(Intent.ACTION_VIEW, uri)

        val chooser = Intent.createChooser(intent, "Open with")
        startActivity(chooser)
    }

    private fun updateJoinButton(event: Event) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (event.maxParticipants == -1) {
            binding.eventDetailsBTNJoin.visibility = android.view.View.GONE
            return
        }

        binding.eventDetailsBTNJoin.visibility = android.view.View.VISIBLE

        if (userId != null && userId in event.participants) {
            binding.eventDetailsBTNJoin.text = "Cancel Registration"
            binding.eventDetailsBTNJoin.isEnabled = true
            return
        }

        if (event.participants.size >= event.maxParticipants) {
            binding.eventDetailsBTNJoin.text = "Event is Full"
            binding.eventDetailsBTNJoin.isEnabled = false
            return
        }

        binding.eventDetailsBTNJoin.text = "Join Event"
        binding.eventDetailsBTNJoin.isEnabled = true

    }

    private fun updateSavedButton() {
        val iconRes = if (isSaved) {
            R.drawable.saved
        } else {
            R.drawable.empty_save
        }

        binding.eventDetailsBTNSaved.setImageResource(iconRes)
    }
    private fun joinEvent(event: Event, userId: String) {
        val eventRef = db.collection("events").document(event.id)

        // when 2 or more users try to join
        db.runTransaction { transaction ->
            val snapshot = transaction.get(eventRef)
            val freshEvent = snapshot.toObject(Event::class.java)?.copy(id = snapshot.id)
                ?: throw Exception("Failed to load event")

            if (userId in freshEvent.participants) {
                throw Exception("User already joined")
            }

            if (freshEvent.maxParticipants != -1 &&
                freshEvent.participants.size >= freshEvent.maxParticipants
            ) {
                throw Exception("Event is full")
            }

            val updatedParticipants = freshEvent.participants + userId
            transaction.update(eventRef, "participants", updatedParticipants)
        }.addOnSuccessListener {
            Toast.makeText(this, "Joined successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, e.message ?: "Failed to join event", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelJoin(event: Event, userId: String) {
        val eventRef = db.collection("events").document(event.id)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(eventRef)
            val freshEvent = snapshot.toObject(Event::class.java)?.copy(id = snapshot.id)
                ?: throw Exception("Failed to load event")

            if (userId !in freshEvent.participants) {
                throw Exception("User is not registered")
            }

            val updatedParticipants = freshEvent.participants.filter { it != userId }
            transaction.update(eventRef, "participants", updatedParticipants)
        }.addOnSuccessListener {
            Toast.makeText(this, "Registration cancelled", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, e.message ?: "Failed to cancel registration", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleSaveEvent(eventId: String) {
        lifecycleScope.launch {
            try {
                isSaved = savedEventsManager.toggleSaved(eventId)
                updateSavedButton()
            } catch (e: Exception) {
                Toast.makeText(
                    this@EventDetailsActivity,
                    "Failed to update saved event",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun checkIfEventSaved(eventId: String) {
        lifecycleScope.launch {
            try {
                isSaved = savedEventsManager.isEventSaved(eventId)
                updateSavedButton()
            } catch (e: Exception) {
                Toast.makeText(
                    this@EventDetailsActivity,
                    "Failed to check saved state",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun formatDate(dateTimeMillis: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return sdf.format(Date(dateTimeMillis))
    }

    private fun formatTime(dateTimeMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(dateTimeMillis))
    }

    override fun onDestroy() {
        super.onDestroy()
        eventListener?.remove()
    }
}