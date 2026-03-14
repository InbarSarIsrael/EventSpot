package com.eventspot.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.eventspot.app.databinding.ActivityEventDetailsBinding
import com.eventspot.app.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailsBinding
    private val db = FirebaseFirestore.getInstance()
    private var currentEvent: Event? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        setupNavigateButton()
        setupJoinButton()
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

//    private fun loadEventData() {
//        val eventId = intent.getStringExtra("event_id")
//
//        if (eventId.isNullOrEmpty()) {
//            Toast.makeText(this, "Event ID is missing", Toast.LENGTH_SHORT).show()
//            finish()
//            return
//        }
//
//        db.collection("events")
//            .document(eventId)
//            .get()
//            .addOnSuccessListener { document ->
//                if (document.exists()) {
//                    val event = document.toObject(Event::class.java)?.copy(id = document.id)
//
//                    if (event != null) {
//                        currentEvent = event
//                        bindEventData(event)
//                    } else {
//                        Toast.makeText(this, "Failed to load event data", Toast.LENGTH_SHORT).show()
//                        finish()
//                    }
//                } else {
//                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
//                    finish()
//                }
//            }
//            .addOnFailureListener { exception ->
//                // Log.e("EventDetailsActivity", "Error loading event", exception)
//                Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show()
//                finish()
//            }
//    }

    private fun loadEventData() {
        val eventId = intent.getStringExtra("event_id")

        if (eventId.isNullOrEmpty()) {
            Toast.makeText(this, "Event ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchEventById(eventId)
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

        if (event.imageUri.isNotEmpty()) {
            Glide.with(this)
                .load(event.imageUri)
                .into(binding.eventDetailsIMGEvent)
        }
    }

    private fun fetchEventById(eventId: String) {
        db.collection("events")
            .document(eventId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val event = document.toObject(Event::class.java)?.copy(id = document.id)

                    if (event != null) {
                        currentEvent = event
                        bindEventData(event)
                    } else {
                        Toast.makeText(this, "Failed to load event data", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show()
                finish()
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

    private fun joinEvent(event: Event, userId: String) {
        if (event.maxParticipants != -1 && event.participants.size >= event.maxParticipants) {
            Toast.makeText(this, "Event is full", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedParticipants = event.participants + userId

        db.collection("events")
            .document(event.id)
            .update("participants", updatedParticipants)
            .addOnSuccessListener {
                currentEvent = event.copy(participants = updatedParticipants)
                updateJoinButton(currentEvent!!)
                Toast.makeText(this, "Joined successfully", Toast.LENGTH_SHORT).show()
                fetchEventById(event.id)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to join event", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cancelJoin(event: Event, userId: String) {
        val updatedParticipants = event.participants.filter { it != userId }

        db.collection("events")
            .document(event.id)
            .update("participants", updatedParticipants)
            .addOnSuccessListener {
                currentEvent = event.copy(participants = updatedParticipants)
                updateJoinButton(currentEvent!!)
                Toast.makeText(this, "Registration cancelled", Toast.LENGTH_SHORT).show()
                fetchEventById(event.id)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to cancel registration", Toast.LENGTH_SHORT).show()
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
}