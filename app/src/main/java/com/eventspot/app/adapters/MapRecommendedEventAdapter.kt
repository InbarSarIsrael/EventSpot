package com.eventspot.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.eventspot.app.R
import com.eventspot.app.databinding.ItemMapRecommendedEventBinding
import com.eventspot.app.model.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapRecommendedEventAdapter(
    private val onEventClick: (Event) -> Unit
) : ListAdapter<Event, MapRecommendedEventAdapter.RecommendedEventViewHolder>(
    RecommendedEventDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendedEventViewHolder {
        val binding = ItemMapRecommendedEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecommendedEventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecommendedEventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecommendedEventViewHolder(
        private val binding: ItemMapRecommendedEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.mapRecommendedLBLTitle.text = event.name
            binding.mapRecommendedLBLCategories.text = event.categories.take(2).joinToString(" • ")
            binding.mapRecommendedLBLDate.text = formatDate(event.dateTimeMillis, event.hasTime)
            binding.mapRecommendedLBLAddress.text = event.address

            bindImage(event.imageUri)

            binding.root.setOnClickListener {
                onEventClick(event)
            }
        }

        private fun bindImage(imageUrl: String) {
            if (imageUrl == "unavailable_photo") {
                binding.mapRecommendedIMG.setImageResource(R.drawable.unavailable_photo)
                return
            }

            Glide.with(binding.mapRecommendedIMG.context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.unavailable_photo)
                .error(R.drawable.unavailable_photo)
                .into(binding.mapRecommendedIMG)
        }

        private fun formatDate(dateTimeMillis: Long, hasTime: Boolean): String {
            if (!hasTime) {
                val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                return formatter.format(Date(dateTimeMillis))
            }

            val formatter = SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault())
            return formatter.format(Date(dateTimeMillis))
        }
    }

    class RecommendedEventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }
    }
}
