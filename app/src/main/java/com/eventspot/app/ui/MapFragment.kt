package com.eventspot.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.eventspot.app.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import android.annotation.SuppressLint

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private var gMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val telAviv = LatLng(32.0853, 34.7818) // fallback
    private val defaultZoom = 13f
    private val userZoom = 16f

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showUserLocation()
            } else {
                showTelAvivFallback(showToast = true)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_container) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        gMap = map

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        if (hasFineLocationPermission()) {
            showUserLocation()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    @SuppressLint("MissingPermission")
    private fun showUserLocation() {
        val map = gMap ?: return

        if (!hasFineLocationPermission()) {
            showTelAvivFallback(showToast = true)
            return
        }

        try {
            map.isMyLocationEnabled = true
        } catch (_: SecurityException) {
            showTelAvivFallback(showToast = true)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, userZoom))
                } else {
                    showTelAvivFallback(showToast = true)
                }
            }
            .addOnFailureListener {
                showTelAvivFallback(showToast = true)
            }
    }

    private fun showTelAvivFallback(showToast: Boolean) {
        val map = gMap ?: return
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(telAviv, defaultZoom))

        if (showToast) {
            Toast.makeText(
                requireContext(),
                "Location denied, showing Tel Aviv",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}