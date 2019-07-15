package com.leonard.policereport.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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

    private var map: GoogleMap? = null

    private val mapContentSubject = BehaviorSubject.create<MapsViewModel.ViewState.Content>()
    private var disposeBag = CompositeDisposable()

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MapsViewModel::class.java)
        setupObserver()
        setupMonthList()
    }

    private fun setupObserver() {
        viewModel.loadingEventsState.observe(this, Observer { state ->
            when (state) {
                is MapsViewModel.ViewState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    map?.clear()
                    snackbar?.dismiss()
                }
                is MapsViewModel.ViewState.Error -> {
                    progressBar.visibility = View.GONE
                    snackbar = Snackbar.make(rootView, getString(R.string.something_wrong), Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry)) { viewModel.loadCrimeEvents() }
                        .apply{show()}
                }
                is MapsViewModel.ViewState.Empty -> {
                    progressBar.visibility = View.GONE
                    snackbar = Snackbar.make(rootView, getString(R.string.no_events), Snackbar.LENGTH_SHORT)
                        .apply{show()}
                }
                is MapsViewModel.ViewState.Content -> {
                    progressBar.visibility = View.GONE
                    mapContentSubject.onNext(state)
                }
            }
        })
    }

    private fun setupMonthList() {
        //hardcoded to July, 2019
        val currentMonth = 7
        val months = IntArray(currentMonth) { currentMonth - it }.toTypedArray()
        val adapter = ArrayAdapter<Int>(this, R.layout.month_item, months)

        monthSpinner.adapter = adapter
        monthSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, view: View?, position: Int, p3: Long) {
                    viewModel.month = months[position]
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

        monthSpinner.setSelection(currentMonth - viewModel.month)
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
            map?.let {
                viewModel.location = it.cameraPosition.target
                viewModel.zoom = it.cameraPosition.zoom
                viewModel.setBounds(it.projection.visibleRegion.latLngBounds)
            }
        } catch (exception: Exception) {
            Log.e("MAP_EXCEPTION", exception.message.orEmpty())
        }
    }

    private fun drawMarkers(content: MapsViewModel.ViewState.Content) {
        map?.run {
            content.events.forEach { event ->
                val circleOptions = CircleOptions().center(LatLng(event.location.latitude, event.location.longitude))
                    .radius(1.0)
                    .fillColor(Color.RED)
                    .strokeColor(Color.RED)
                addCircle(circleOptions)
            }
        }
    }
}
