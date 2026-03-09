package com.eventspot.app.ui

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.eventspot.app.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import android.annotation.SuppressLint
import android.content.Intent
import com.eventspot.app.EventDetailsActivity
import com.eventspot.app.adapters.EventInfoWindowAdapter
import com.eventspot.app.utilities.UserLocationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.eventspot.app.model.Event
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Marker

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private var gMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationHelper: UserLocationHelper

    private val telAviv = LatLng(32.0853, 34.7818) // fallback
    private val defaultZoom = 13f
    private val userZoom = 16f
    private val db = FirebaseFirestore.getInstance()

    private val eventMarkers = mutableMapOf<String, Marker>()
    private val eventsById = mutableMapOf<String, Event>()
    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showUserLocation()
            } else {
                showTelAvivFallback(showToast = true)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationHelper = UserLocationHelper(requireContext(), fusedLocationClient)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_container) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        gMap = map

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        map.setInfoWindowAdapter(
            EventInfoWindowAdapter(requireContext(), eventsById)
        )

        setupMarkerClicks()
        setupInfoWindowClicks()

        if (locationHelper.hasFineLocationPermission()) {
            showUserLocation()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        loadEventsToMap()
    }
    @SuppressLint("MissingPermission")
    private fun showUserLocation() {
        val map = gMap ?: return

        if (!locationHelper.hasFineLocationPermission()) {
            showTelAvivFallback(showToast = true)
            return
        }

        try {
            map.isMyLocationEnabled = true
        } catch (_: SecurityException) {
            showTelAvivFallback(showToast = true)
            return
        }

        locationHelper.getUserLocation { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, userZoom))
            } else {
                showTelAvivFallback(showToast = true)
            }
        }
    }

    private fun loadEventsToMap() {
        val map = gMap ?: return

        db.collection("events")
            .get()
            .addOnSuccessListener { result ->
                val incomingEventIds = mutableSetOf<String>()

                for (document in result.documents) {
                    val event = document.toObject(Event::class.java)?.copy(id = document.id)
                        ?: continue

                    if (event.lat == 0.0 && event.lng == 0.0) continue

                    eventsById[event.id] = event
                    incomingEventIds.add(event.id)

                    val eventLatLng = LatLng(event.lat, event.lng)
                    val existingMarker = eventMarkers[event.id]

                    if (existingMarker == null) {
                        val newMarker = map.addMarker(
                            MarkerOptions()
                                .position(eventLatLng)
                                .title(event.name)
                                .snippet(event.address)
                        )

                        if (newMarker != null) {
                            newMarker.tag = event.id
                            eventMarkers[event.id] = newMarker
                        }
                    } else { //need to check about update event
                        existingMarker.position = eventLatLng
                        existingMarker.title = event.name
                        existingMarker.snippet = event.address
                        existingMarker.tag = event.id
                    }
                }

                val idsToRemove = eventMarkers.keys - incomingEventIds

                for (eventId in idsToRemove) {
                    eventMarkers[eventId]?.remove()
                    eventMarkers.remove(eventId)
                    eventsById.remove(eventId)
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Failed to load events",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun setupInfoWindowClicks() {
        val map = gMap ?: return

        map.setOnInfoWindowClickListener { marker ->
            val eventId = marker.tag as? String ?: return@setOnInfoWindowClickListener

            val intent = Intent(requireContext(), EventDetailsActivity::class.java).apply {
                putExtra("event_id", eventId)
            }

            startActivity(intent)
        }
    }

    private fun setupMarkerClicks() {
        val map = gMap ?: return

        map.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }
    }

    private fun showTelAvivFallback(showToast: Boolean) {
        val map = gMap ?: return
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(telAviv, defaultZoom))

        if (showToast) {
            Toast.makeText(
                requireContext(),
                "Failed to get location, showing Tel Aviv",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        gMap = null
        eventMarkers.clear()
        eventsById.clear()
    }
}