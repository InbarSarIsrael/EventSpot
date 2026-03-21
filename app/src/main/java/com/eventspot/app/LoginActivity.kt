package com.eventspot.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.eventspot.app.repository.FirestoreUserRepository
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val userRepository = FirestoreUserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val currentUser = FirebaseAuth.getInstance().currentUser

        // If no user is currently signed in, launch sign-in flow
        if(currentUser == null) {
            signIn()
        }
        else{
            createUserAndNavigate(currentUser)
        }
    }

    // See: https://developer.android.com/training/basics/intents/result
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { res ->
        onSignInResult(res)

    }

    private fun signIn(){
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
        )

        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.drawable.background_photo) // Optional app logo in sign-in screen
            .setTheme(R.style.LoginTheme) // Optional theme for styling
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser

            if (user != null) {
                createUserAndNavigate(user)
            } else {
                Toast.makeText(
                    this,
                    "Error: user is null after login",
                    Toast.LENGTH_LONG
                ).show()
                signIn()
            }
        } else {
            Toast.makeText(
                this,
                "Error: failed logging in",
                Toast.LENGTH_LONG
            ).show()
            signIn()
        }
    }



    private fun createUserAndNavigate(user: FirebaseUser) {
        lifecycleScope.launch {
            try {
                userRepository.createUserIfNotExists(
                    userId = user.uid,
                    email = user.email ?: "",
                    name = user.displayName ?: ""
                )

                val hasRole = userRepository.hasUserRole(user.uid)

                if (hasRole) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                } else {
                    startActivity(Intent(this@LoginActivity, ChooseRoleActivity::class.java))
                }

                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Error saving user data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}