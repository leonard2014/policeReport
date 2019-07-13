package com.leonard.policereport.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.snackbar.Snackbar
import com.leonard.policereport.R
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_maps.*
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
        viewModel.loadingEventsState.observe(this, Observer { state ->
            when (state) {
                is MapsViewModel.ViewState.Loading ->
                    progressBar.visibility = View.VISIBLE
                is MapsViewModel.ViewState.Error -> {
                    progressBar.visibility = View.GONE
                    Snackbar.make(rootView, "Sorry, something is wrong", Snackbar.LENGTH_SHORT)
                        .show()
                }
                is MapsViewModel.ViewState.Empty -> {
                    progressBar.visibility = View.GONE
                    Snackbar.make(rootView, "No events found", Snackbar.LENGTH_SHORT)
                        .show()
                }
                is MapsViewModel.ViewState.Content -> {
                    progressBar.visibility = View.GONE
                    Snackbar.make(rootView, "${state.events.size} found", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap.apply {
            moveCamera(CameraUpdateFactory.newLatLngZoom(viewModel.location, viewModel.zoom))
            setOnCameraIdleListener(::onCameraIdle)
        }
    }

    private fun onCameraIdle() {
        Log.d("London Map", "onCameraIdle")
        try {
            viewModel.location = map.cameraPosition.target
            viewModel.zoom = map.cameraPosition.zoom
            viewModel.setBounds(map.projection.visibleRegion.latLngBounds)
        } catch (exception: Exception) {
            Log.e("MAP_EXCEPTION", exception.message.orEmpty())
        }
    }
}