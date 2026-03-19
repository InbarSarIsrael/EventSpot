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
import android.app.DatePickerDialog
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import com.eventspot.app.EventDetailsActivity
import com.google.android.gms.location.LocationServices
import com.eventspot.app.utilities.UserLocationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import androidx.lifecycle.lifecycleScope
import com.eventspot.app.model.Event
import com.eventspot.app.repository.FirestoreEventRepository
import com.eventspot.app.utilities.SavedEventsManager
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    private val eventRepository = FirestoreEventRepository()
    private val savedEventsManager = SavedEventsManager()
    private var savedEventIds: Set<String> = emptySet()
    private lateinit var eventAdapter: EventAdapter

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationHelper: UserLocationHelper

    private var allEvents: List<Event> = emptyList()
    private var filteredEvents: List<Event> = emptyList()
    private var eventsListener: ListenerRegistration? = null
    private var showAvailableOnly: Boolean = false
    private var selectedFromDateMillis: Long? = null

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
        setupSearch()
        setupFilters()
        loadEvents()
        checkLocationPermission()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(
            onEventClick = { event ->
                val intent = Intent(requireContext(), EventDetailsActivity::class.java).apply {
                    putExtra("event_id", event.id)
                }
                startActivity(intent)
            },
            onSaveClick = { event ->
                toggleSavedEvent(event.id)
            }
        )

        binding.eventsRVList.layoutManager = LinearLayoutManager(requireContext())
        binding.eventsRVList.adapter = eventAdapter
        binding.eventsRVList.setHasFixedSize(true)
    }

    private fun setupSearch() {
        binding.eventsEDTSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun setupFilters() {
        binding.eventsCBAvailable.setOnCheckedChangeListener { _, isChecked ->
            showAvailableOnly = isChecked
            applyFilters()
        }

        binding.eventsBTNDateFilter.setOnClickListener {
            showDatePicker()
        }

        binding.eventsBTNClearFilters.setOnClickListener {
            clearFilters()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                selectedFromDateMillis = selectedCalendar.timeInMillis

                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.eventsBTNDateFilter.text = formatter.format(selectedCalendar.time)

                applyFilters()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
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
                savedEventIds = savedEventsManager.getSavedEventIds()

                eventsListener?.remove()
                eventsListener = eventRepository.observeEvents { events ->
                    allEvents = events
                    filteredEvents = events
                    applyFilters()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun applyFilters() {
        val searchText = binding.eventsEDTSearch.text.toString().trim().lowercase()

        filteredEvents = allEvents.filter { event ->
            val matchesSearch =
                searchText.isEmpty() ||
                        event.name.lowercase().contains(searchText) ||
                        event.categories.any { it.lowercase().contains(searchText) }

            val isAvailableEvent =
                event.maxParticipants == -1 || event.participants.size < event.maxParticipants

            val matchesAvailability =
                !showAvailableOnly || isAvailableEvent

            val matchesDate =
                selectedFromDateMillis == null || event.dateTimeMillis >= selectedFromDateMillis!!

            matchesSearch && matchesAvailability && matchesDate
        }

        eventAdapter.submitList(filteredEvents)
        eventAdapter.updateSavedEventIds(savedEventIds)

        if (filteredEvents.isEmpty()) {
            binding.eventsLBLEmptyState.visibility = View.VISIBLE
            binding.eventsRVList.visibility = View.GONE
        } else {
            binding.eventsLBLEmptyState.visibility = View.GONE
            binding.eventsRVList.visibility = View.VISIBLE
        }
    }

    private fun clearFilters() {
        showAvailableOnly = false
        selectedFromDateMillis = null

        binding.eventsCBAvailable.isChecked = false
        binding.eventsEDTSearch.text?.clear()
        binding.eventsBTNDateFilter.text = "Date"

        applyFilters()
    }
    private fun toggleSavedEvent(eventId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val isNowSaved = savedEventsManager.toggleSaved(eventId)

                savedEventIds = if (isNowSaved) {
                    savedEventIds + eventId
                } else {
                    savedEventIds - eventId
                }

                applyFilters()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun refreshSavedEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                savedEventIds = savedEventsManager.getSavedEventIds()
                applyFilters()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSavedEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        eventsListener?.remove()
        eventsListener = null
        _binding = null
    }


}