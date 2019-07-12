package com.leonard.policereport.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import javax.inject.Inject

class MapsViewModel : ViewModel() {
    //Latitude and Longitude of London City
    var location = LatLng(51.5131808,-0.090536)
    var zoom = 16f

    lateinit var bounds : LatLngBounds
}

class MapsViewModelFactory
@Inject constructor() : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        if (modelClass.isAssignableFrom(MapsViewModel::class.java)) {
            MapsViewModel() as T
        } else {
            throw IllegalArgumentException("unknown model class $modelClass")
        }
}