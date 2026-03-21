package com.eventspot.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.eventspot.app.databinding.ActivityChooseRoleBinding
import com.eventspot.app.model.UserRole
import com.eventspot.app.repository.FirestoreUserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ChooseRoleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChooseRoleBinding
    private val userRepository = FirestoreUserRepository()

    private var selectedRole: UserRole? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityChooseRoleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickButton()
    }

    private fun setupClickButton() {
        binding.cardExplorer.setOnClickListener {
            selectedRole = UserRole.EVENT_EXPLORER
            updateSelectedRoleUI()
        }

        binding.cardProducer.setOnClickListener {
            selectedRole = UserRole.PRODUCER
            updateSelectedRoleUI()
        }

        binding.btnContinue.setOnClickListener {
            saveRoleAndContinue()
        }
    }

    private fun updateSelectedRoleUI() {
        val selectedStrokeColor = ContextCompat.getColor(this, R.color.role_selected_stroke)
        val defaultStrokeColor = ContextCompat.getColor(this, R.color.role_default_stroke)

        binding.cardExplorer.strokeColor =
            if (selectedRole == UserRole.EVENT_EXPLORER) selectedStrokeColor else defaultStrokeColor

        binding.cardProducer.strokeColor =
            if (selectedRole == UserRole.PRODUCER) selectedStrokeColor else defaultStrokeColor
    }

    private fun saveRoleAndContinue() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedRole == null) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                userRepository.saveUserRole(userId, selectedRole!!)
                startActivity(Intent(this@ChooseRoleActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ChooseRoleActivity,
                    "Failed to save role: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}