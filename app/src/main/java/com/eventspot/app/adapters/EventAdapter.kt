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
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    private var userLocation: Location? = null

    fun updateUserLocation(location: Location?) {
        userLocation = location
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

            binding.eventIGMSaved.setImageResource(
                if (event.isSaved) R.drawable.saved else R.drawable.empty_save
            )

            bindImage(event.imageUri)
            bindDistance(event)
            bindCategories(event.categories)

            binding.root.setOnClickListener {
                onEventClick(event)
            }
        }

        private fun bindImage(imageUrl: String) {
            val imageSource = imageUrl.ifBlank {
                R.drawable.unavailable_photo
            }
            Glide.with(binding.eventIMG.context)
                .load(imageSource)
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