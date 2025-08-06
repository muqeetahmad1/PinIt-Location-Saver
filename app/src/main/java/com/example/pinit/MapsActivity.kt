package com.example.pinit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions



class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Default location (Lahore)
        val defaultLatLng = LatLng(31.5204, 74.3587)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 10f))

        // When user taps on the map
        map.setOnMapClickListener { latLng ->
            map.clear()
            map.addMarker(MarkerOptions().position(latLng).title("Selected Location"))

            val resultIntent = Intent().apply {
                putExtra("lat", latLng.latitude)
                putExtra("lng", latLng.longitude)
            }

            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
