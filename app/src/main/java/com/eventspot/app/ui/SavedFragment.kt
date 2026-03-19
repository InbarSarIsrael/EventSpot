package com.eventspot.app.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.eventspot.app.EventDetailsActivity
import com.eventspot.app.adapters.EventAdapter
import com.eventspot.app.databinding.FragmentSavedBinding
import com.eventspot.app.repository.FirestoreEventRepository
import com.eventspot.app.utilities.SavedEventsManager
import kotlinx.coroutines.launch

class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!

    private val eventRepository = FirestoreEventRepository()
    private val savedEventsManager = SavedEventsManager()
    private var savedEventIds: Set<String> = emptySet()
    private lateinit var eventAdapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadSavedEvents()
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
                showRemoveSavedEventDialog(event.id)
            }
        )

        binding.savedRVList.layoutManager = LinearLayoutManager(requireContext())
        binding.savedRVList.adapter = eventAdapter
        binding.savedRVList.setHasFixedSize(true)
    }

    private fun loadSavedEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val events = eventRepository.getAllEvents()
                savedEventIds = savedEventsManager.getSavedEventIds()

                val savedEvents = events.filter { it.id in savedEventIds }

                eventAdapter.submitList(savedEvents)
                eventAdapter.updateSavedEventIds(savedEventIds)

                updateEmptyState(savedEvents.isEmpty())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showRemoveSavedEventDialog(eventId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove this saved event?")
            .setPositiveButton("Remove") { _, _ ->
                toggleSavedEvent(eventId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleSavedEvent(eventId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                savedEventsManager.toggleSaved(eventId)
                loadSavedEvents()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.savedTVEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.savedRVList.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadSavedEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}