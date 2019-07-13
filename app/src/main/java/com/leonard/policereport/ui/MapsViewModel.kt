package com.leonard.policereport.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.leonard.policereport.model.CrimeEvent
import com.leonard.policereport.repository.Repository
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject

class MapsViewModel(private val repository: Repository) : ViewModel() {
    sealed class ViewState {
        object Loading : ViewState()
        class Error(val exception: Throwable) : ViewState()
        object Empty : ViewState()
        data class Content(val events: List<CrimeEvent>) : ViewState()
    }

    private val _loadingEventsState = MutableLiveData<ViewState>().apply { value = ViewState.Loading }
    val loadingEventsState: LiveData<ViewState> = _loadingEventsState

    //Latitude and Longitude of London City
    var location = LatLng(51.5131808, -0.090536)
    var zoom = 18f

    private var bounds = LatLngBounds(LatLng(0.0, 0.0), LatLng(0.0, 0.0))
    fun setBounds(value: LatLngBounds) {
        bounds = value
        loadCrimeEvents()
    }

    private var disposeBag = CompositeDisposable()

    override fun onCleared() {
        super.onCleared()
        disposeBag.clear()
    }

    fun loadCrimeEvents() {
        _loadingEventsState.postValue(ViewState.Loading)

        disposeBag += repository.getCrimeEvents(
            bounds.southwest.latitude, bounds.southwest.longitude,
            bounds.northeast.latitude, bounds.northeast.latitude
        ).subscribe(
            { events ->
                if (!events.isEmpty()) {
                    _loadingEventsState.postValue(ViewState.Content(events))
                } else {
                    _loadingEventsState.postValue(ViewState.Empty)
                }
            },
            { error ->
                _loadingEventsState.postValue(ViewState.Error(error))
            }
        )
    }
}

class MapsViewModelFactory
@Inject constructor(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        if (modelClass.isAssignableFrom(MapsViewModel::class.java)) {
            MapsViewModel(repository) as T
        } else {
            throw IllegalArgumentException("unknown model class $modelClass")
        }
}