package com.eventspot.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.eventspot.app.databinding.ActivityEventDetailsBinding
import com.eventspot.app.model.Event
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailsBinding

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        loadEventData()
    }

    private fun setupBackButton() {
        binding.eventDetailsBTNBack.setOnClickListener {
            finish()
        }
    }

    private fun loadEventData() {
        val eventId = intent.getStringExtra("event_id")

        if (eventId.isNullOrEmpty()) {
            Toast.makeText(this, "Event ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("events")
            .document(eventId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val event = document.toObject(Event::class.java)?.copy(id = document.id)

                    if (event != null) {
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
            .addOnFailureListener { exception ->
                // Log.e("EventDetailsActivity", "Error loading event", exception)
                Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show()
                finish()
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

        if (event.imageUri.isNotEmpty()) {
            Glide.with(this)
                .load(event.imageUri)
                .into(binding.eventDetailsIMGEvent)
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