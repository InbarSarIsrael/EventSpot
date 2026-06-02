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
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ChooseRoleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChooseRoleBinding
    private val userRepository = FirestoreUserRepository()

    private var selectedRole: UserRole? = null
    private val selectedCategories = linkedSetOf<String>()

    private companion object {
        const val MAX_SELECTED_CATEGORIES = 5
    }

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
        setupInterestChips()
        updateSelectedCategoriesCount()
        updateContinueButtonState()
    }

    private fun setupClickButton() {
        binding.cardExplorer.setOnClickListener {
            selectedRole = UserRole.EVENT_EXPLORER
            updateSelectedRoleUI()
            updateContinueButtonState()
        }

        binding.cardProducer.setOnClickListener {
            selectedRole = UserRole.PRODUCER
            updateSelectedRoleUI()
            updateContinueButtonState()
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

    private fun setupInterestChips() {
        for (index in 0 until binding.chipGroupInterests.childCount) {
            val chip = binding.chipGroupInterests.getChildAt(index) as? Chip ?: continue

            chip.setOnCheckedChangeListener { button, isChecked ->
                val category = button.text.toString()

                if (isChecked) {
                    if (selectedCategories.size >= MAX_SELECTED_CATEGORIES) {
                        button.isChecked = false
                        Toast.makeText(
                            this,
                            getString(R.string.choose_categories_limit_message, MAX_SELECTED_CATEGORIES),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnCheckedChangeListener
                    }

                    selectedCategories.add(category)
                } else {
                    selectedCategories.remove(category)
                }

                updateSelectedCategoriesCount()
                updateContinueButtonState()
            }
        }
    }

    private fun updateSelectedCategoriesCount() {
        binding.tvSelectedCategoriesCount.text = getString(
            R.string.selected_categories_count,
            selectedCategories.size,
            MAX_SELECTED_CATEGORIES
        )
    }

    private fun updateContinueButtonState() {
        val canContinue = selectedRole != null && selectedCategories.isNotEmpty()

        binding.btnContinue.isEnabled = canContinue
        binding.btnContinue.alpha = if (canContinue) 1f else 0.5f
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

        if (selectedCategories.isEmpty()) {
            Toast.makeText(this, R.string.please_choose_at_least_one_category, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                userRepository.saveUserOnboardingPreferences(
                    userId = userId,
                    role = selectedRole!!,
                    preferredCategories = selectedCategories.toList()
                )
                startActivity(Intent(this@ChooseRoleActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ChooseRoleActivity,
                    "Failed to save preferences: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
