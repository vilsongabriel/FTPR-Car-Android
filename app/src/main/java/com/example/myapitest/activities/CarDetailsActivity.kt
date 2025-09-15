package com.example.myapitest.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapitest.R
import com.example.myapitest.databinding.ActivityCarDetailsBinding
import com.example.myapitest.model.Car
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.Result
import com.example.myapitest.service.apiCall
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class CarDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCarDetailsBinding
    private lateinit var car: Car
    private lateinit var googleMap: GoogleMap

    private val carFormLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchCarDetails(car.id)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val carId = intent.getStringExtra("car_id")
        if (carId == null) {
            showToast(getString(R.string.error_loading_car_details))
            finish()
            return
        }

        setupToolbar()
        setupClickListeners()
        fetchCarDetails(carId)
        setupMap()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_car_details)
    }

    private fun setupClickListeners() {
        binding.editButton.setOnClickListener {
            editCar()
        }

        binding.deleteButton.setOnClickListener {
            confirmDelete()
        }
    }

    private fun setupMap() {
        showMapLoader()
        
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun fetchCarDetails(carId: String) {
        lifecycleScope.launch {
            when (val result = apiCall { RetrofitClient.apiService.getItem(carId) }) {
                is Result.Success -> {
                    car = result.data.value
                    displayCarDetails()
                }
                is Result.Error -> {
                    showToast(getString(R.string.error_loading_details, result.message))
                    finish()
                }
            }
        }
    }

    private fun displayCarDetails() {
        binding.apply {
            carName.text = car.name
            carYear.text = car.year
            carLicense.text = car.licence

            Picasso.get()
                .load(car.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .resize(800, 600)
                .centerCrop()
                .into(carImage)
        }

        if (::googleMap.isInitialized) {
            updateMapLocation()
        } else {
            showMapLoader()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = false
        }

        if (::car.isInitialized) {
            updateMapLocation()
        }
    }

    private fun updateMapLocation() {
        val location = LatLng(car.place.lat, car.place.long)
        
        googleMap.clear()
        googleMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(car.name)
                .snippet(getString(R.string.snippet_car_location))
        )
        
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(location, 15f)
        )
        
        binding.root.postDelayed({
            hideMapLoader()
        }, 800)
    }

    private fun showMapLoader() {
        binding.mapLoadingLayout.visibility = android.view.View.VISIBLE
    }

    private fun hideMapLoader() {
        binding.mapLoadingLayout.visibility = android.view.View.GONE
    }

    private fun editCar() {
        val intent = Intent(this, CarFormActivity::class.java)
        intent.putExtra("car_id", car.id)
        carFormLauncher.launch(intent)
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_delete_title))
            .setMessage(getString(R.string.confirm_delete_message, car.name))
            .setPositiveButton(getString(R.string.button_delete)) { _, _ ->
                deleteCar()
            }
            .setNegativeButton(getString(R.string.button_cancel), null)
            .show()
    }

    private fun deleteCar() {
        lifecycleScope.launch {
            when (val result = apiCall { RetrofitClient.apiService.deleteItem(car.id) }) {
                is Result.Success -> {
                    showToast(getString(R.string.car_deleted_success))
                    setResult(RESULT_OK)
                    finish()
                }
                is Result.Error -> {
                    showToast(getString(R.string.error_deleting_car, result.message))
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}