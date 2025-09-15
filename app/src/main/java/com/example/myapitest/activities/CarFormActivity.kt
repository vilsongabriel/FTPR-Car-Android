package com.example.myapitest.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapitest.R
import com.example.myapitest.databinding.ActivityCarFormBinding
import com.example.myapitest.model.Car
import com.example.myapitest.model.CarPlace
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.Result
import com.example.myapitest.service.apiCall
import com.example.myapitest.utils.FirebaseStorageUtils
import com.example.myapitest.utils.PermissionUtils
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.util.*

class CarFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarFormBinding
    private var selectedImageUri: Uri? = null
    private var currentImageUrl: String? = null
    private var carId: String? = null
    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0

    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 100
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.carImageView.setImageURI(it)
        }
    }

    private val mapLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                selectedLat = data.getDoubleExtra("lat", 0.0)
                selectedLng = data.getDoubleExtra("lng", 0.0)
                binding.latEditText.setText(selectedLat.toString())
                binding.lngEditText.setText(selectedLng.toString())
                
                binding.selectLocationButton.text = getString(R.string.button_change_location)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        carId = intent.getStringExtra("car_id")
        
        setupToolbar()
        setupClickListeners()
        
        if (carId != null) {
            loadCarData(carId!!)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = if (carId != null) getString(R.string.title_edit_car) else getString(R.string.title_add_car)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupClickListeners() {
        binding.carImageView.setOnClickListener {
            if (PermissionUtils.hasStoragePermission(this)) {
                imagePickerLauncher.launch("image/*")
            } else {
                requestStoragePermission()
            }
        }

        binding.selectLocationButton.setOnClickListener {
            val intent = Intent(this, SelectPlaceActivity::class.java)
            intent.putExtra("select_location", true)
            if (selectedLat != 0.0 && selectedLng != 0.0) {
                intent.putExtra("lat", selectedLat)
                intent.putExtra("lng", selectedLng)
            }
            mapLauncher.launch(intent)
        }

        binding.saveButton.setOnClickListener {
            saveCar()
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            PermissionUtils.getStoragePermissions(),
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    imagePickerLauncher.launch("image/*")
                } else {
                    showToast(getString(R.string.error_gallery_permission))
                }
            }
        }
    }

    private fun loadCarData(carId: String) {
        showLoading(true)
        
        lifecycleScope.launch {
            when (val result = apiCall { RetrofitClient.apiService.getItem(carId) }) {
                is Result.Success -> {
                    val car = result.data.value
                    populateFields(car)
                }
                is Result.Error -> {
                    showToast(getString(R.string.error_loading_car_data, result.message))
                    finish()
                }
            }
            showLoading(false)
        }
    }

    private fun populateFields(car: Car) {
        binding.apply {
            nameEditText.setText(car.name)
            yearEditText.setText(car.year)
            licenceEditText.setText(car.licence)
            latEditText.setText(car.place.lat.toString())
            lngEditText.setText(car.place.long.toString())
            
            selectedLat = car.place.lat
            selectedLng = car.place.long
            currentImageUrl = car.imageUrl
            
            if (selectedLat != 0.0 && selectedLng != 0.0) {
                selectLocationButton.text = getString(R.string.button_change_location)
            }
            
            Picasso.get()
                .load(car.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(carImageView)
        }
    }

    private fun saveCar() {
        if (!validateFields()) return
        
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val imageUrl = if (selectedImageUri != null) {
                    FirebaseStorageUtils.uploadImage(selectedImageUri!!)
                } else {
                    currentImageUrl ?: throw Exception(getString(R.string.error_no_image_selected))
                }
                
                val car = Car(
                    id = carId ?: UUID.randomUUID().toString(),
                    imageUrl = imageUrl,
                    year = binding.yearEditText.text.toString().trim(),
                    name = binding.nameEditText.text.toString().trim(),
                    licence = binding.licenceEditText.text.toString().trim(),
                    place = CarPlace(
                        lat = binding.latEditText.text.toString().toDoubleOrNull() ?: 0.0,
                        long = binding.lngEditText.text.toString().toDoubleOrNull() ?: 0.0
                    )
                )
                
                val result = if (carId != null) {
                    apiCall { RetrofitClient.apiService.updateItem(carId!!, car) }
                } else {
                    apiCall { RetrofitClient.apiService.addItem(car) }
                }
                
                when (result) {
                    is Result.Success -> {
                        showToast(getString(R.string.car_saved_success))
                        setResult(RESULT_OK)
                        finish()
                    }
                    is Result.Error -> {
                        showToast(getString(R.string.error_saving_car, result.message))
                    }
                }
                
            } catch (e: Exception) {
                showToast(getString(R.string.error_generic, e.message))
            }
            
            showLoading(false)
        }
    }

    private fun validateFields(): Boolean {
        binding.apply {
            if (nameEditText.text.toString().trim().isEmpty()) {
                nameInputLayout.error = getString(R.string.error_name_required)
                return false
            }
            nameInputLayout.error = null
            
            if (yearEditText.text.toString().trim().isEmpty()) {
                yearInputLayout.error = getString(R.string.error_year_required)
                return false
            }
            yearInputLayout.error = null
            
            if (licenceEditText.text.toString().trim().isEmpty()) {
                licenceInputLayout.error = getString(R.string.error_license_required)
                return false
            }
            licenceInputLayout.error = null
            
            val lat = latEditText.text.toString().toDoubleOrNull()
            if (lat == null) {
                latInputLayout.error = getString(R.string.error_invalid_latitude)
                return false
            }
            latInputLayout.error = null
            
            val lng = lngEditText.text.toString().toDoubleOrNull()
            if (lng == null) {
                lngInputLayout.error = getString(R.string.error_invalid_longitude)
                return false
            }
            lngInputLayout.error = null
            
            if (lat == 0.0 && lng == 0.0) {
                showToast(getString(R.string.error_select_location))
                return false
            }
            
            if (selectedImageUri == null && currentImageUrl == null) {
                showToast(getString(R.string.error_select_image))
                return false
            }
        }
        
        return true
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.saveButton.isEnabled = !show
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}