package com.example.myapitest.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapitest.R
import com.example.myapitest.databinding.ItemCarLayoutBinding
import com.example.myapitest.model.Car
import com.squareup.picasso.Picasso

class CarAdapter(
    private val cars: List<Car>,
    private val onItemClick: (Car, String) -> Unit
) : RecyclerView.Adapter<CarAdapter.CarViewHolder>() {

    companion object {
        const val ACTION_EDIT = "edit"
        const val ACTION_DELETE = "delete"
        const val ACTION_VIEW_DETAILS = "view_details"
    }

    inner class CarViewHolder(private val binding: ItemCarLayoutBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(car: Car) {
            binding.apply {
                model.text = car.name
                year.text = car.year
                license.text = car.licence
                
                Picasso.get()
                    .load(car.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .resize(200, 200)
                    .centerCrop()
                    .into(image)
                
                root.setOnClickListener {
                    onItemClick(car, ACTION_VIEW_DETAILS)
                }
                
                root.setOnLongClickListener {
                    showOptionsDialog(car)
                    true
                }
            }
        }
        
        private fun showOptionsDialog(car: Car) {
            val context = binding.root.context
            val options = arrayOf(context.getString(R.string.option_edit), context.getString(R.string.option_delete))
            
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.dialog_options_title))
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> onItemClick(car, ACTION_EDIT)
                        1 -> confirmDelete(car)
                    }
                }
                .show()
        }
        
        private fun confirmDelete(car: Car) {
            val context = binding.root.context
            
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.dialog_confirm_delete_title))
                .setMessage(context.getString(R.string.dialog_confirm_delete_message, car.name))
                .setPositiveButton(context.getString(R.string.option_delete)) { _, _ ->
                    onItemClick(car, ACTION_DELETE)
                }
                .setNegativeButton(context.getString(R.string.action_cancel), null)
                .show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val binding = ItemCarLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        holder.bind(cars[position])
    }

    override fun getItemCount() = cars.size
}