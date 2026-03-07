package com.eventspot.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.eventspot.app.databinding.ActivityEventDetailsBinding

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailsBinding

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
        val eventName = intent.getStringExtra("event_name") ?: ""
        val eventProducer = intent.getStringExtra("event_producer") ?: ""
        val eventDate = intent.getStringExtra("event_date") ?: ""
        val eventTime = intent.getStringExtra("event_time") ?: ""
        val eventLocation = intent.getStringExtra("event_location") ?: ""
        val eventDescription = intent.getStringExtra("event_description") ?: ""
        val eventCategories = intent.getStringArrayListExtra("event_categories") ?: arrayListOf()

        binding.eventDetailsLBLName.text = eventName
        binding.eventDetailsLBLProducer.text = "Producer: $eventProducer"
        binding.eventDetailsLBLDate.text = "Date: $eventDate"
        binding.eventDetailsLBLTime.text = "Time: $eventTime"
        binding.eventDetailsLBLLocation.text = "Location: $eventLocation"
        binding.eventDetailsLBLCategories.text = "Categories: ${eventCategories.joinToString(", ")}"
        binding.eventDetailsLBLDescription.text = eventDescription
    }
}