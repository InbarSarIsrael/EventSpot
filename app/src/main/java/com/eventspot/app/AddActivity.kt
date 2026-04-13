package com.eventspot.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.eventspot.app.databinding.ActivityAddBinding
import com.eventspot.app.model.Event
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID


class AddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddBinding
    private val addressPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleAddressPickerResult(result.resultCode, result.data)
        }
    private val selectedCategories = mutableListOf<String>()
    private var selectedDateTimeMillis: Long? = null
    private var selectedAddress: String? = null
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var selectedImageUri: Uri? = null
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                binding.addIMG.setImageURI(uri)
                binding.addUploadText.visibility = View.GONE
            }
        }
    private val availableCategories = listOf(
        "Music", "Party", "Nightlife", "Festival",
        "Food", "Drinks", "Art", "Exhibition",
        "Culture", "Cinema", "Theater", "Stand-up",
        "Workshop", "Networking", "Technology", "Business",
        "Sports", "Fitness", "Outdoor", "Family"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }

        binding.addIMGContainer.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        setupCategoriesDropdown()
        setupDateTimePicker()
        setupAddressPicker()
        setupAddButton()
    }

    private fun setupCategoriesDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            availableCategories
        )

        binding.addACTCategories.setAdapter(adapter)

        binding.addACTCategories.setOnItemClickListener { parent, _, position, _ ->
            val category = parent.getItemAtPosition(position).toString()

            if (selectedCategories.contains(category)) {
                binding.addACTCategories.setText("")
                return@setOnItemClickListener
            }

            if (selectedCategories.size >= 3) {
                Toast.makeText(this, "You can choose up to 3 categories", Toast.LENGTH_SHORT).show()
                binding.addACTCategories.setText("")
                return@setOnItemClickListener
            }

            selectedCategories.add(category)
            addCategoryChip(category)
            binding.addACTCategories.setText("")
        }
    }

    private fun addCategoryChip(category: String) {
        val chip = Chip(this).apply {
            text = category
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                binding.addChipGroupCategories.removeView(this)
                selectedCategories.remove(category)
            }
        }

        binding.addChipGroupCategories.addView(chip)
    }

    private fun setupDateTimePicker() {
        binding.addEDTDateAndTime.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val now = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                showTimePicker(year, month, dayOfMonth)
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker(year: Int, month: Int, dayOfMonth: Int) {
        val now = Calendar.getInstance()

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (selectedCalendar.timeInMillis < System.currentTimeMillis()) {
                    Toast.makeText(this, "You cannot choose a past time", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }

                selectedDateTimeMillis = selectedCalendar.timeInMillis

                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                binding.addEDTDateAndTime.setText(formatter.format(selectedCalendar.time))
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true
        )

        timePickerDialog.show()
    }

    private fun setupAddressPicker() {
        binding.addEDTAddress.setOnClickListener {
            openAddressAutocomplete()
        }
    }

    private fun openAddressAutocomplete() {
        val fields: List<Place.Field> = listOf(
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION
        )

        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.FULLSCREEN,
            fields
        ).build(this)

        addressPickerLauncher.launch(intent)
    }

    private fun handleAddressPickerResult(resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK || data == null) {
            return
        }

        val place = Autocomplete.getPlaceFromIntent(data)

        val address = place.formattedAddress ?: place.displayName ?: ""
        val location = place.location

        if (address.isBlank() || location == null) {
            binding.addLBLAddress.error = "Please choose a valid place from the list"
            selectedAddress = null
            selectedLat = null
            selectedLng = null
            binding.addEDTAddress.setText("")
            return
        }

        selectedAddress = address
        selectedLat = location.latitude
        selectedLng = location.longitude

        binding.addEDTAddress.setText(address)
        binding.addLBLAddress.error = null
    }

    private fun setupAddButton() {
        binding.addEventBtn.setOnClickListener {

            val eventName = binding.addEDTName.text.toString().trim()
            if (eventName.isEmpty()) {
                binding.addEDTName.error = "Event name is required"
                binding.addEDTName.requestFocus()
                return@setOnClickListener
            }

            if (selectedCategories.isEmpty()) {
                Toast.makeText(this, "Please choose at least one category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val maxParticipants = validateMaxParticipants() ?: return@setOnClickListener

            val dateTimeMillis = selectedDateTimeMillis ?: run {
                binding.addLBLDateAndTime.error = "Date and time are required"
                return@setOnClickListener
            }

            val address = selectedAddress ?: run {
                binding.addLBLAddress.error = "Address is required"
                return@setOnClickListener
            }

            val lat = selectedLat ?: return@setOnClickListener
            val lng = selectedLng ?: return@setOnClickListener

            uploadImageToFirebase { imageUri ->

                val db = FirebaseFirestore.getInstance()
                val eventRef = db.collection("events").document()

                val event = Event(
                    id = eventRef.id,
                    name = eventName,
                    dateTimeMillis = dateTimeMillis,
                    address = address,
                    description = binding.addEDTDescription.text.toString().trim(),
                    categories = selectedCategories,
                    lat = lat,
                    lng = lng,
                    maxParticipants = maxParticipants,
                    imageUri = imageUri ?: "unavailable_photo"
                )

                eventRef.set(event)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Event added!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to add event", Toast.LENGTH_SHORT).show()
                    }
            }

        }
    }

    private fun uploadImageToFirebase(onComplete: (String?) -> Unit) {
        val image = selectedImageUri ?: return onComplete(null)

        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("event_images/${System.currentTimeMillis()}.jpg")

        imageRef.putFile(image)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        onComplete(uri.toString())
                    }
                    .addOnFailureListener {
                        //Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show()
                        // maybe log
                        onComplete(null)
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                onComplete(null)
            }
    }

    private fun validateMaxParticipants(): Int? {
        val text = binding.addEDTMaxParticipants.text?.toString()?.trim()

        if (text.isNullOrEmpty()) {
            binding.addLBLMaxParticipants.error = null
            return -1
        }

        val value = text.toIntOrNull()

        if (value == null || value < 1) {
            binding.addLBLMaxParticipants.error = "Value must be 1 or greater"
            return null
        } else {
            binding.addLBLMaxParticipants.error = null
            return value
        }
    }
}