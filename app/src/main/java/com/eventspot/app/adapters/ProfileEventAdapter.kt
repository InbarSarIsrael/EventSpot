package com.eventspot.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.eventspot.app.R
import com.eventspot.app.databinding.ItemProfileEventBinding
import com.eventspot.app.model.Event

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileEventAdapter(
    private val showManagementButtons: Boolean,
    private val onEventClick: (Event) -> Unit,
    private val onEditClick: (Event) -> Unit,
    private val onDeleteClick: (Event) -> Unit
) : ListAdapter<Event, ProfileEventAdapter.ProfileEventViewHolder>(ProfileEventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileEventViewHolder {
        val binding = ItemProfileEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProfileEventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileEventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProfileEventViewHolder(
        private val binding: ItemProfileEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.profileLBLTitle.text = event.name
            binding.profileLBLDateTime.text = formatDateTime(event.dateTimeMillis)
            binding.profileLBLAddress.text = event.address

            bindImage(event.imageUri)
            bindManagementButtons(event)

            binding.root.setOnClickListener {
                onEventClick(event)
            }
        }

        private fun bindImage(imageUrl: String) {
            if (imageUrl == "unavailable_photo") {
                binding.profileIMG.setImageResource(R.drawable.unavailable_photo)
                return
            }

            Glide.with(binding.profileIMG.context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.unavailable_photo)
                .error(R.drawable.unavailable_photo)
                .into(binding.profileIMG)
        }

        private fun bindManagementButtons(event: Event) {
            if (showManagementButtons) {
                binding.profileBTNEdit.visibility = View.VISIBLE
                binding.profileBTNDelete.visibility = View.VISIBLE

                bindEditButton(event)
                bindDeleteButton(event)

            } else {
                binding.profileBTNEdit.visibility = View.GONE
                binding.profileBTNDelete.visibility = View.GONE
            }
        }
        private fun bindEditButton(event: Event) {
            binding.profileBTNEdit.setOnClickListener {
                onEditClick(event)
            }
        }

        private fun bindDeleteButton(event: Event) {
            binding.profileBTNDelete.setOnClickListener {
                onDeleteClick(event)
            }
        }

        private fun formatDateTime(dateTimeMillis: Long): String {
            val formatter = SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault())
            return formatter.format(Date(dateTimeMillis))
        }
    }

    class ProfileEventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }
    }
}