package com.eventspot.app.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.eventspot.app.R
import com.eventspot.app.model.Event
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class EventInfoWindowAdapter(
    context: Context,
    private val eventsById: Map<String, Event>
) : GoogleMap.InfoWindowAdapter {

    private val window: View =
        LayoutInflater.from(context).inflate(R.layout.view_event_info_window, null)

    override fun getInfoWindow(marker: Marker): View {
        render(marker, window)
        return window
    }

    override fun getInfoContents(marker: Marker): View {
        render(marker, window)
        return window
    }

    private fun render(marker: Marker, view: View) {
        val imageView = view.findViewById<ImageView>(R.id.info_IMG)
        val titleView = view.findViewById<TextView>(R.id.info_LBL_title)
        val addressView = view.findViewById<TextView>(R.id.info_LBL_address)

        val eventId = marker.tag as? String
        val event = eventId?.let { eventsById[it] }

        titleView.text = event?.name ?: marker.title ?: "Event"
        addressView.text = event?.address ?: marker.snippet ?: ""

        if (!event?.imageUri.isNullOrBlank()) {
            Glide.with(view.context)
                .load(event.imageUri)
                .placeholder(R.drawable.unavailable_photo)
                .error(R.drawable.unavailable_photo)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.unavailable_photo)
        }
    }
}