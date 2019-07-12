package com.leonard.policereport.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.lang.IllegalArgumentException
import javax.inject.Inject

class MapsViewModel : ViewModel() {
    data class ViewState(val bounds: LatLngBounds)

    //Latidue and Longitude of London City
    private var bounds = LatLngBounds(LatLng(51.5152943, -0.0840178), LatLng(51.5154454, -0.0776729))

    private val _viewState = MutableLiveData<ViewState>().apply { value = ViewState(bounds)}
    val viewState: LiveData<ViewState> = _viewState
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