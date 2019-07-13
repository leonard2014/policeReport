package com.leonard.policereport.ui

import android.graphics.Color
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
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.leonard.policereport.R
import dagger.android.AndroidInjection
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.activity_maps.*
import javax.inject.Inject

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    @Inject
    lateinit var viewModelFactory: MapsViewModelFactory

    private lateinit var viewModel: MapsViewModel

    private lateinit var map: GoogleMap

    private val mapContentSubject: BehaviorSubject<MapsViewModel.ViewState.Content> = BehaviorSubject.create()
    private var disposeBag = CompositeDisposable()

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
                    Snackbar.make(rootView, getString(R.string.something_wrong), Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry)) { viewModel.loadCrimeEvents() }
                        .show()
                }
                is MapsViewModel.ViewState.Empty -> {
                    progressBar.visibility = View.GONE
                    Snackbar.make(rootView, getString(R.string.no_events), Snackbar.LENGTH_SHORT)
                        .show()
                }
                is MapsViewModel.ViewState.Content -> {
                    progressBar.visibility = View.GONE
                    mapContentSubject.onNext(state)
                }
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap.apply {
            moveCamera(CameraUpdateFactory.newLatLngZoom(viewModel.location, viewModel.zoom))
            setOnCameraIdleListener(::onCameraIdle)
        }

        disposeBag += mapContentSubject.subscribe(::drawMarkers)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposeBag.clear()
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

    private fun drawMarkers(content: MapsViewModel.ViewState.Content) {
        content.events.forEach { event ->
            val circleOptions = CircleOptions().center(LatLng(event.location.latitude, event.location.longitude))
                .radius(1.0)
                .fillColor(Color.RED)
                .strokeColor(Color.RED)
            map.addCircle(circleOptions)
        }
    }

}
