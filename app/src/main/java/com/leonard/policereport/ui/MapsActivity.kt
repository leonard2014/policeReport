package com.leonard.policereport.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.leonard.policereport.R
import dagger.android.AndroidInjection
import javax.inject.Inject

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    @Inject
    lateinit var viewModelFactory: MapsViewModelFactory

    private lateinit var viewModel: MapsViewModel

    private lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MapsViewModel::class.java)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(viewModel.location, viewModel.zoom))

        map.setOnCameraIdleListener (::onCameraIdle)
    }

    private fun onCameraIdle() {
        Log.d("London Map","onCameraIdle")
        try{
            viewModel.location = map.cameraPosition.target
            viewModel.zoom = map.cameraPosition.zoom
            viewModel.bounds = map.projection.visibleRegion.latLngBounds
        } catch (exception: Exception) {
            Log.e("MAP_EXCEPTION", exception.message.orEmpty())
        }

    }
}
