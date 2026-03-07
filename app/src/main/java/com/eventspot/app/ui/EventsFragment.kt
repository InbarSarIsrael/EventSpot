package com.eventspot.app.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.eventspot.app.adapters.EventAdapter
import com.eventspot.app.databinding.FragmentEventsBinding
import com.eventspot.app.model.Event
import android.Manifest
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.eventspot.app.EventDetailsActivity
import com.google.android.gms.location.LocationServices
import com.eventspot.app.utilities.UserLocationHelper
import com.google.android.gms.location.FusedLocationProviderClient

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!
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
        loadDummyEvents()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadDummyEvents() {
        val dummyEvents = listOf(
            Event(
                id = "1",
                name = "Live Jazz Night",
                producer = "Bar Rubina",
                address = "Bar Rubina, Tel Aviv",
                description = "An intimate live jazz evening with local musicians.",
                categories = listOf("Music", "Nightlife"),
                lat = 32.0733,
                lng = 34.7747,
                imageUrl = "https://static.wixstatic.com/media/740c76_373359e8e9f149a5a020c37c10fdb49b~mv2.jpg/v1/fill/w_640,h_422,al_c,q_80,usm_0.66_1.00_0.01,enc_avif,quality_auto/740c76_373359e8e9f149a5a020c37c10fdb49b~mv2.jpg",
                dateTimeMillis = System.currentTimeMillis()
            ),
            Event(
                id = "2",
                name = "Food Festival",
                producer = "Sarona Market",
                address = "Sarona Market, Tel Aviv",
                description = "Street food festival with top Israeli chefs.",
                categories = listOf("Food", "Festival"),
                lat = 32.0717,
                lng = 34.7873,
                imageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/19/fb/18/d0/sarona-market.jpg?w=1200&h=-1&s=1",
                dateTimeMillis = System.currentTimeMillis()
            ),
            Event(
                id = "3",
                name = "Sunset Yoga",
                producer = "Tel Aviv Port",
                address = "Tel Aviv Port, Tel Aviv",
                description = "Outdoor sunset yoga session by the sea.",
                categories = listOf("Sport", "Wellness"),
                lat = 32.1005,
                lng = 34.7746,
                imageUrl = "https://images.stockcake.com/public/7/e/c/7eccaeb1-9d8d-4b1f-a08e-0d9cea534390_large/sunset-yoga-meditation-stockcake.jpg",
                dateTimeMillis = System.currentTimeMillis()
            ),
            Event(
                id = "4",
                name = "Open Air Movie Night",
                producer = "Azrieli Center",
                address = "Azrieli Center, Tel Aviv",
                description = "Outdoor movie screening on the rooftop with city skyline views.",
                categories = listOf("Cinema", "Nightlife"),
                lat = 32.0740,
                lng = 34.7922,
                imageUrl = "https://static.vecteezy.com/system/resources/thumbnails/074/178/701/small/a-full-red-and-white-striped-container-filled-with-fluffy-popcorn-photo.jpg",
                dateTimeMillis = System.currentTimeMillis()
            )
        )
        eventAdapter.submitList(dummyEvents)
    }
}