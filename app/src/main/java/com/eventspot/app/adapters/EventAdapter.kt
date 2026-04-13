package com.eventspot.app.adapters

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.eventspot.app.R
import com.eventspot.app.databinding.ItemEventBinding
import com.eventspot.app.model.Event
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAdapter(
    private val onEventClick: (Event) -> Unit,
    private val onSaveClick: (Event) -> Unit
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    private var userLocation: Location? = null
    private var savedEventIds: Set<String> = emptySet()

    fun updateUserLocation(location: Location?) {
        userLocation = location
        notifyDataSetChanged()
    }

    fun updateSavedEventIds(savedIds: Set<String>) {
        savedEventIds = savedIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.eventLBLTitle.text = event.name
            binding.eventLBLProducer.text = event.producer
            binding.eventLBLDateTime.text = formatDateTime(event.dateTimeMillis)
            binding.eventLBLAddress.text = event.address
            binding.eventLBLDesc.text = event.description

            bindImage(event.imageUri)
            bindDistance(event)
            bindCategories(event.categories)
            bindSavedIcon(event)

            binding.root.setOnClickListener {
                onEventClick(event)
            }
        }

        private fun bindImage(imageUrl: String) {
            if (imageUrl == "unavailable_photo") {
                binding.eventIMG.setImageResource(R.drawable.unavailable_photo)
                return
            }

            Glide.with(binding.eventIMG.context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.unavailable_photo)
                .error(R.drawable.unavailable_photo)
                .into(binding.eventIMG)
        }

        private fun bindDistance(event: Event) {
            val currentLocation = userLocation

            if (currentLocation == null || (event.lat == 0.0 && event.lng == 0.0)) {
                binding.eventLBLDistance.text = "Distance not available"
                return
            }

            val eventLocation = Location("event").apply {
                latitude = event.lat
                longitude = event.lng
            }

            val distanceInMeters = currentLocation.distanceTo(eventLocation)
            binding.eventLBLDistance.text = formatDistance(distanceInMeters)
        }

        private fun bindCategories(categories: List<String>) {
            binding.eventCHIPGROUPCategories.removeAllViews()

            if (categories.isEmpty()) {
                binding.eventCHIPGROUPCategories.visibility = View.GONE
                return
            }

            binding.eventCHIPGROUPCategories.visibility = View.VISIBLE

            categories.take(3).forEach { category ->
                val chip = Chip(binding.root.context).apply {
                    text = category
                    isClickable = false
                    isCheckable = false
                    setEnsureMinTouchTargetSize(false)
                }
                binding.eventCHIPGROUPCategories.addView(chip)
            }
        }

        private fun bindSavedIcon(event: Event) {
            val isSaved = savedEventIds.contains(event.id)

            val iconRes = if (isSaved) {
                R.drawable.saved
            } else {
                R.drawable.empty_save
            }

            binding.eventBTNSaved.setImageResource(iconRes)

            binding.eventBTNSaved.setOnClickListener {
                onSaveClick(event)
            }
        }

        private fun formatDateTime(dateTimeMillis: Long): String {
            val formatter = SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault())
            return formatter.format(Date(dateTimeMillis))
        }

        private fun formatDistance(distanceInMeters: Float): String {
            return if (distanceInMeters < 1000) {
                "${distanceInMeters.toInt()} m away"
            } else {
                val distanceInKm = distanceInMeters / 1000f
                String.format(Locale.getDefault(), "%.1f km away", distanceInKm)
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }
    }
}