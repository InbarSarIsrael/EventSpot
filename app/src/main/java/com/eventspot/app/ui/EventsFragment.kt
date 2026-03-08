package com.eventspot.app.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.eventspot.app.adapters.EventAdapter
import com.eventspot.app.databinding.FragmentEventsBinding
import android.Manifest
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.eventspot.app.EventDetailsActivity
import com.google.android.gms.location.LocationServices
import com.eventspot.app.utilities.UserLocationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import androidx.lifecycle.lifecycleScope
import com.eventspot.app.repository.FirestoreEventRepository
import kotlinx.coroutines.launch

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    private val eventRepository = FirestoreEventRepository()

    private lateinit var eventAdapter: EventAdapter

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationHelper: UserLocationHelper

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                fetchUserLocation()
            } else {
                eventAdapter.updateUserLocation(null)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationHelper = UserLocationHelper(requireContext(), fusedLocationClient)

        setupRecyclerView()
        loadEvents()
        checkLocationPermission()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            val intent = Intent(requireContext(), EventDetailsActivity::class.java).apply {
                putExtra("event_id", event.id)
            }
            startActivity(intent)
        }


        binding.eventsRVList.layoutManager = LinearLayoutManager(requireContext())
        binding.eventsRVList.adapter = eventAdapter
        binding.eventsRVList.setHasFixedSize(true)
    }

    private fun checkLocationPermission() {
        if (locationHelper.hasFineLocationPermission()) {
            fetchUserLocation()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchUserLocation() {
        locationHelper.getUserLocation { location ->
            eventAdapter.updateUserLocation(location)
        }
    }

    private fun loadEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                eventRepository.seedDummyEventsIfNeeded()
                val events = eventRepository.getAllEvents()
                eventAdapter.submitList(events)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}