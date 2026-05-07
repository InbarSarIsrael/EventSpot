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
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.eventspot.app.AddActivity
import com.eventspot.app.EventDetailsActivity
import com.eventspot.app.adapters.EventInfoWindowAdapter
import com.eventspot.app.databinding.FragmentMapBinding
import com.eventspot.app.utilities.UserLocationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.eventspot.app.model.Event
import com.eventspot.app.repository.FirestoreEventRepository
import com.eventspot.app.utilities.NotificationPermissionHelper
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Marker
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private var gMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val telAviv = LatLng(32.0853, 34.7818) // fallback
    private val defaultZoom = 13f
    private val userZoom = 13f
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val eventRepository = FirestoreEventRepository()
    private var eventsListener: ListenerRegistration? = null
    private val eventMarkers = mutableMapOf<String, Marker>()
    private val eventsById = mutableMapOf<String, Event>()

    private lateinit var locationHelper: UserLocationHelper
    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showUserLocation()
            } else {
                showTelAvivFallback(showToast = true)
            }
        }

    private lateinit var notificationPermissionHelper: NotificationPermissionHelper
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            lifecycleScope.launch {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

                if (granted) {
                    Firebase.firestore.collection("users")
                        .document(userId)
                        .update(
                            mapOf(
                                "notificationPreferences.enabled" to true,
                                "notificationPreferences.joinedEventUpdates" to true,
                                "notificationPreferences.dailyNewEvents" to false
                            )
                        )
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationHelper = UserLocationHelper(requireContext(), fusedLocationClient)
        notificationPermissionHelper = NotificationPermissionHelper(requireContext())

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_container) as SupportMapFragment

        mapFragment.getMapAsync(this)


        setupAddButton()
        askNotificationPermissionIfNeeded()
    }


    private fun setupAddButton() {
        binding.mapBTNAdd.visibility = View.GONE

        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { userDocument ->
                val role = userDocument.getString("role").orEmpty()

                if (role == "PRODUCER") {
                    binding.mapBTNAdd.visibility = View.VISIBLE
                    binding.mapBTNAdd.setOnClickListener {
                        startActivity(Intent(requireContext(), AddActivity::class.java))
                    }
                }
            }
            .addOnFailureListener {
                binding.mapBTNAdd.visibility = View.GONE
            }
    }

    private fun askNotificationPermissionIfNeeded() {
        if (notificationPermissionHelper.hasNotificationPermission()) return
        if (notificationPermissionHelper.wasNotificationPromptShown()) return

        notificationPermissionHelper.markNotificationPromptAsShown()
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
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

        eventsListener?.remove()

        eventsListener = eventRepository.observeEvents { events ->
            renderEventsOnMap(map, events)
        }
    }

    private fun renderEventsOnMap(map: GoogleMap, events: List<Event>) {
        val incomingEventIds = mutableSetOf<String>()

        for (event in events) {
            incomingEventIds.add(event.id)
            eventsById[event.id] = event
            addOrUpdateEventMarker(map, event)
        }

        removeOldMarkers(incomingEventIds)
    }

    private fun addOrUpdateEventMarker(map: GoogleMap, event: Event) {
        val eventLatLng = getEventMarkerPosition(event)
        val markerColor = getEventMarkerColor(event)
        val markerSnippet = getEventMarkerSnippet(event)

        val existingMarker = eventMarkers[event.id]

        if (existingMarker == null) {
            val newMarker = map.addMarker(
                MarkerOptions()
                    .position(eventLatLng)
                    .title(event.name)
                    .snippet(markerSnippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            )

            newMarker?.let {
                it.tag = event.id
                eventMarkers[event.id] = it
            }
        } else {
            existingMarker.position = eventLatLng
            existingMarker.title = event.name
            existingMarker.snippet = markerSnippet
            existingMarker.setIcon(BitmapDescriptorFactory.defaultMarker(markerColor))
            existingMarker.tag = event.id
        }
    }

    private fun getEventMarkerPosition(event: Event): LatLng {
        return if (hasExactLocation(event)) {
            LatLng(event.lat, event.lng)
        } else {
            telAviv
        }
    }

    private fun getEventMarkerColor(event: Event): Float {
        return if (hasExactLocation(event)) {
            BitmapDescriptorFactory.HUE_RED
        } else {
            BitmapDescriptorFactory.HUE_AZURE
        }
    }

    private fun getEventMarkerSnippet(event: Event): String {
        return if (hasExactLocation(event)) {
            event.address
        } else {
            "General location: Tel Aviv"
        }
    }

    private fun hasExactLocation(event: Event): Boolean {
        return event.lat != 0.0 && event.lng != 0.0
    }

    private fun removeOldMarkers(incomingEventIds: Set<String>) {
        val idsToRemove = eventMarkers.keys - incomingEventIds

        for (eventId in idsToRemove) {
            eventMarkers[eventId]?.remove()
            eventMarkers.remove(eventId)
            eventsById.remove(eventId)
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

        eventsListener?.remove()
        eventsListener = null

        gMap = null
        eventMarkers.clear()
        eventsById.clear()
    }
}