package com.example.myapitest.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapitest.R
import com.example.myapitest.adapters.CarAdapter
import com.example.myapitest.databinding.ActivityMainBinding
import com.example.myapitest.model.Car
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.Result
import com.example.myapitest.service.apiCall
import com.example.myapitest.utils.AuthUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var carAdapter: CarAdapter
    private val cars = mutableListOf<Car>()

    private val carDetailsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchItems()
        }
    }

    private val carFormLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchItems()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!AuthUtils.isUserLoggedIn()) {
            navigateToLogin()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        if (AuthUtils.isUserLoggedIn()) {
            fetchItems()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.title_my_cars)
    }

    private fun setupRecyclerView() {
        carAdapter = CarAdapter(cars) { car, action ->
            when (action) {
                CarAdapter.ACTION_EDIT -> editCar(car)
                CarAdapter.ACTION_DELETE -> deleteCar(car)
                CarAdapter.ACTION_VIEW_DETAILS -> viewCarDetails(car)
            }
        }
        
        binding.recyclerView.apply {
            adapter = carAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupClickListeners() {
        binding.addCta.setOnClickListener {
            val intent = Intent(this, CarFormActivity::class.java)
            carFormLauncher.launch(intent)
        }
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchItems()
        }
    }

    private fun fetchItems() {
        binding.swipeRefreshLayout.isRefreshing = true
        
        lifecycleScope.launch {
            when (val result = apiCall { RetrofitClient.apiService.getItems() }) {
                is Result.Success -> {
                    cars.clear()
                    cars.addAll(result.data)
                    carAdapter.notifyDataSetChanged()
                }
                is Result.Error -> {
                    showToast(getString(R.string.error_loading_cars, result.message))
                }
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun editCar(car: Car) {
        val intent = Intent(this, CarFormActivity::class.java)
        intent.putExtra("car_id", car.id)
        carFormLauncher.launch(intent)
    }

    private fun deleteCar(car: Car) {
        lifecycleScope.launch {
            when (val result = apiCall { RetrofitClient.apiService.deleteItem(car.id) }) {
                is Result.Success -> {
                    cars.remove(car)
                    carAdapter.notifyDataSetChanged()
                    showToast(getString(R.string.car_deleted_success))
                }
                is Result.Error -> {
                    showToast(getString(R.string.error_deleting_car, result.message))
                }
            }
        }
    }

    private fun viewCarDetails(car: Car) {
        val intent = Intent(this, CarDetailsActivity::class.java)
        intent.putExtra("car_id", car.id)
        carDetailsLauncher.launch(intent)
    }

    private fun logout() {
        AuthUtils.logout()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}