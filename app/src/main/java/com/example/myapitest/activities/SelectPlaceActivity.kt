package com.example.myapitest.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapitest.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class SelectPlaceActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var isSelectMode = false
    private var selectedLocation: LatLng? = null
    private var originalLocation: LatLng? = null
    private var hasUserInteracted = false
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_place)

        setupToolbar()
        setupConfirmButton()

        isSelectMode = intent.getBooleanExtra("select_location", false)
        
        if (isSelectMode) {
            supportActionBar?.title = getString(R.string.title_select_location)
            findViewById<com.google.android.material.button.MaterialButton>(R.id.confirmButton).visibility = android.view.View.VISIBLE
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_select_place)
        }
    }

    private fun setupConfirmButton() {
        val confirmButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.confirmButton)
        confirmButton.setOnClickListener {
            confirmSelection()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        setupMap()
        
        val lat = intent.getDoubleExtra("lat", 0.0)
        val lng = intent.getDoubleExtra("lng", 0.0)
        val title = intent.getStringExtra("title")

        if (lat != 0.0 && lng != 0.0) {
            val location = LatLng(lat, lng)
            
            if (isSelectMode) {
                selectedLocation = location
                originalLocation = location
                mMap.addMarker(MarkerOptions().position(location).title(getString(R.string.marker_selected_location)))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            } else {
                mMap.addMarker(MarkerOptions().position(location).title(title ?: getString(R.string.marker_current_location)))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            }
        } else {
            getCurrentLocation()
        }
    }

    private fun setupMap() {
        mMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        } else {
            requestLocationPermission()
        }

        if (isSelectMode) {
            mMap.setOnMapClickListener { latLng ->
                mMap.clear()
                
                selectedLocation = latLng
                mMap.addMarker(MarkerOptions().position(latLng).title(getString(R.string.marker_selected_location)))
                
                hasUserInteracted = true
                updateConfirmButtonState()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLocation = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
                    
                    if (isSelectMode && selectedLocation == null) {
                        selectedLocation = currentLocation
                        originalLocation = currentLocation
                        mMap.addMarker(MarkerOptions().position(currentLocation).title(getString(R.string.marker_current_location)))
                        updateConfirmButtonState()
                    }
                }
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupMap()
                    getCurrentLocation()
                } else {
                    Toast.makeText(this, getString(R.string.error_location_permission_denied), Toast.LENGTH_SHORT).show()
                    if (isSelectMode) {
                        val defaultLocation = LatLng(-23.5505, -46.6333)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                    }
                }
            }
        }
    }

    private fun updateConfirmButtonState() {
        val confirmButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.confirmButton)
        
        val shouldEnable = hasUserInteracted || (selectedLocation != null && originalLocation != null && 
                          (selectedLocation!!.latitude != originalLocation!!.latitude || 
                           selectedLocation!!.longitude != originalLocation!!.longitude))
        
        confirmButton.isEnabled = shouldEnable
    }

    private fun confirmSelection() {
        selectedLocation?.let { location ->
            val resultIntent = Intent().apply {
                putExtra("lat", location.latitude)
                putExtra("lng", location.longitude)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } ?: run {
            Toast.makeText(this, getString(R.string.error_select_location_on_map), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}