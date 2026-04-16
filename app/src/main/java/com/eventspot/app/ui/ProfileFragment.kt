package com.eventspot.app.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eventspot.app.AddActivity
import com.eventspot.app.EventDetailsActivity
import com.eventspot.app.LoginActivity
import com.eventspot.app.adapters.ProfileEventAdapter
import com.eventspot.app.databinding.FragmentProfileBinding
import com.eventspot.app.model.Event
import com.eventspot.app.model.UserRole
import com.eventspot.app.repository.FirestoreUserRepository
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var joinedAdapter: ProfileEventAdapter
    private lateinit var createdAdapter: ProfileEventAdapter

    private var isJoinedExpanded = false
    private var isCreatedExpanded = false

    private val userRepository = FirestoreUserRepository()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickButton()
        setupRecyclerViews()
        setupSectionClicks()
        loadUserRole()
    }

    private fun setupRecyclerViews() {
        setupRecyclerViewJoined()
        setupRecyclerViewCreated()
    }

    private fun setupRecyclerViewJoined() {
        joinedAdapter = ProfileEventAdapter(
            showManagementButtons = false,
            onEventClick = { event ->
                val intent = Intent(requireContext(), EventDetailsActivity::class.java).apply {
                    putExtra("event_id", event.id)
                }
                startActivity(intent)
            },
            onEditClick = { },
            onDeleteClick = { }
        )

        binding.profileRVJoined.layoutManager = LinearLayoutManager(requireContext())
        binding.profileRVJoined.adapter = joinedAdapter
        binding.profileRVJoined.setHasFixedSize(true)
    }

    private fun setupRecyclerViewCreated() {
        createdAdapter = ProfileEventAdapter(
            showManagementButtons = true,
            onEventClick = { event ->
                val intent = Intent(requireContext(), EventDetailsActivity::class.java).apply {
                    putExtra("event_id", event.id)
                }
                startActivity(intent)
            },
            onEditClick = { event ->
                val intent = Intent(requireContext(), AddActivity::class.java).apply {
                    putExtra("event_id", event.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { event ->
                showDeleteDialog(event)
            }
        )

        binding.profileRVCreated.layoutManager = LinearLayoutManager(requireContext())
        binding.profileRVCreated.adapter = createdAdapter
        binding.profileRVCreated.setHasFixedSize(true)
    }

    private fun setupSectionClicks() {
        binding.profileLayoutJoinedHeader.setOnClickListener {
            isJoinedExpanded = !isJoinedExpanded

            binding.profileRVJoined.visibility =
                if (isJoinedExpanded && joinedAdapter.itemCount > 0) View.VISIBLE else View.GONE

            binding.profileTVJoinedEmpty.visibility =
                if (isJoinedExpanded && joinedAdapter.itemCount == 0) View.VISIBLE else View.GONE

            binding.profileIMGJoinedArrow.rotation = if (isJoinedExpanded) 180f else 0f
        }

        binding.profileLayoutCreatedHeader.setOnClickListener {
            isCreatedExpanded = !isCreatedExpanded

            binding.profileRVCreated.visibility =
                if (isCreatedExpanded && createdAdapter.itemCount > 0) View.VISIBLE else View.GONE

            binding.profileTVCreatedEmpty.visibility =
                if (isCreatedExpanded && createdAdapter.itemCount == 0) View.VISIBLE else View.GONE

            binding.profileIMGCreatedArrow.rotation = if (isCreatedExpanded) 180f else 0f
        }
    }

    private fun loadUserRole() {
        val currentUserId = auth.currentUser?.uid ?: return

        loadJoinedEvents(currentUserId)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val role = userRepository.getUserRole(currentUserId)

                if (role == UserRole.PRODUCER) {
                    binding.profileLayoutCreatedSection.visibility = View.VISIBLE
                    loadCreatedEvents(currentUserId)
                } else {
                    binding.profileLayoutCreatedSection.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.profileLayoutCreatedSection.visibility = View.GONE
            }
        }
    }

    private fun loadJoinedEvents(currentUserId: String) {
        db.collection("events")
            .whereArrayContains("participants", currentUserId)
            .get()
            .addOnSuccessListener { result ->
                val events = result.documents.mapNotNull { document ->
                    document.toObject(Event::class.java)?.copy(id = document.id)
                }

                joinedAdapter.submitList(events)

                if (isJoinedExpanded) {
                    binding.profileRVJoined.visibility =
                        if (events.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.profileTVJoinedEmpty.visibility =
                        if (events.isEmpty()) View.VISIBLE else View.GONE
                }
            }
            .addOnFailureListener {
                Log.e("ProfileFragment", "Failed to load joined events", it)
            }
    }

    private fun loadCreatedEvents(currentUserId: String) {
        db.collection("events")
            .whereEqualTo("producerId", currentUserId)
            .get()
            .addOnSuccessListener { result ->
                val events = result.documents.mapNotNull { document ->
                    document.toObject(Event::class.java)?.copy(id = document.id)
                }

                createdAdapter.submitList(events)

                if (isCreatedExpanded) {
                    binding.profileRVCreated.visibility =
                        if (events.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.profileTVCreatedEmpty.visibility =
                        if (events.isEmpty()) View.VISIBLE else View.GONE
                }
            }
            .addOnFailureListener {
                Log.e("ProfileFragment", "Failed to load created events", it)
            }
    }

    private fun showDeleteDialog(event: Event) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete this event?")
            .setMessage("This action cannot be undone")
            .setPositiveButton("Delete") { _, _ ->
                deleteEvent(event)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEvent(event: Event) {
        db.collection("events")
            .document(event.id)
            .delete()
            .addOnSuccessListener {
                loadUserRole()
            }
            .addOnFailureListener {
                Log.e("ProfileFragment", "Failed to delete event", it)
            }
    }

    private fun setupClickButton() {
        binding.profileBTNLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        val currentActivity = activity ?: return
        val credentialManager = CredentialManager.create(currentActivity)

        AuthUI.getInstance()
            .signOut(currentActivity)
            .addOnCompleteListener {

                val intent = Intent(currentActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                currentActivity.startActivity(intent)
                currentActivity.finish()

                lifecycleScope.launch {
                    try {
                        credentialManager.clearCredentialState(
                            ClearCredentialStateRequest()
                        )
                    } catch (e: Exception) {
                        Log.e("Logout", "Failed to clear credentials", e)
                    }
                }
            }
    }

    override fun onResume() {
        super.onResume()
        loadUserRole()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}