package com.eventspot.app.utilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import android.annotation.SuppressLint

class UserLocationHelper(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {

    fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun getUserLocation(onResult: (Location?) -> Unit) {
        if (!ensureLocationPermission(onResult)) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onResult(location)
                } else {
                    getCurrentUserLocation(onResult)
                }
            }
            .addOnFailureListener {
                getCurrentUserLocation(onResult)
            }
    }


    @SuppressLint("MissingPermission")
    private fun getCurrentUserLocation(onResult: (Location?) -> Unit) {
        if (!ensureLocationPermission(onResult)) return

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedLocationClient.getCurrentLocation(request, null)
            .addOnSuccessListener { location ->
                onResult(location)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    private fun ensureLocationPermission(onResult: (Location?) -> Unit): Boolean{
        if (!hasFineLocationPermission()) {
            onResult(null)
            return false
        }
        return true
    }
}