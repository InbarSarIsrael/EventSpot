package com.eventspot.app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.eventspot.app.LoginActivity
import com.eventspot.app.databinding.FragmentProfileBinding
import com.firebase.ui.auth.AuthUI
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!


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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}