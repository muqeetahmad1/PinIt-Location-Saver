package com.example.pinit

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: LocationDatabase
    private lateinit var adapter: LocationAdapter // NEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestLocationPermission()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        db = LocationDatabase.getDatabase(this)

        showSavedLocations()

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            val options = arrayOf("Use Current Location", "Pick from Map")
            AlertDialog.Builder(this)
                .setTitle("Add New Location")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> getCurrentLocation()
                        1 -> {
                            val intent = Intent(this, MapsActivity::class.java)
                            startActivityForResult(intent, 200)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showSavedLocations() {
        val recyclerView = findViewById<RecyclerView>(R.id.locationRecyclerView)
        val searchView = findViewById<SearchView>(R.id.searchView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = DefaultItemAnimator()

        CoroutineScope(Dispatchers.IO).launch {
            val allLocations = db.locationDao().getAllLocations()
            withContext(Dispatchers.Main) {
                adapter = LocationAdapter(allLocations.toMutableList(), this@MainActivity)
                recyclerView.adapter = adapter

                // Search setup
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = false
                    override fun onQueryTextChange(newText: String?): Boolean {
                        adapter.filter.filter(newText)
                        return true
                    }
                })

                // Swipe to delete
                val itemTouchHelper = ItemTouchHelper(object :
                    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean = false

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        val position = viewHolder.adapterPosition
                        val location = adapter.locations[position]

                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Delete Location")
                            .setMessage("Are you sure you want to delete \"${location.name}\"?")
                            .setPositiveButton("Yes") { _, _ ->
                                removeLocation(location)
                                adapter.locations.removeAt(position)
                                adapter.notifyItemRemoved(position)
                            }
                            .setNegativeButton("No") { _, _ ->
                                adapter.notifyItemChanged(position)
                            }
                            .show()
                    }
                })
                itemTouchHelper.attachToRecyclerView(recyclerView)
            }
        }
    }

    fun removeLocation(location: SavedLocation) {
        CoroutineScope(Dispatchers.IO).launch {
            db.locationDao().deleteLocation(location)
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        Toast.makeText(this, "Getting location...", Toast.LENGTH_SHORT).show()
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    showSaveLocationDialog(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Location is null. Make sure GPS is ON.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to get location: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showSaveLocationDialog(lat: Double, lng: Double) {
        val editText = EditText(this)
        editText.hint = "Enter place name"
        AlertDialog.Builder(this)
            .setTitle("Save Location")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val name = editText.text.toString().ifBlank { "Unnamed Location" }
                saveLocation(name, lat, lng)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveLocation(name: String, lat: Double, lng: Double) {
        val location = SavedLocation(
            name = name,
            latitude = lat,
            longitude = lng
        )

        CoroutineScope(Dispatchers.IO).launch {
            db.locationDao().insertLocation(location)
            val updatedList = db.locationDao().getAllLocations()
            withContext(Dispatchers.Main) {
                adapter.updateData(updatedList.toMutableList())
            }
        }

        Toast.makeText(this, "Saved: $name", Toast.LENGTH_SHORT).show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            val lat = data.getDoubleExtra("lat", 0.0)
            val lng = data.getDoubleExtra("lng", 0.0)
            showSaveLocationDialog(lat, lng)
        }
    }
}
