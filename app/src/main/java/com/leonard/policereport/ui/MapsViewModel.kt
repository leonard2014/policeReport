package com.leonard.policereport.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.leonard.policereport.model.CrimeEvent
import com.leonard.policereport.repository.Repository
import io.reactivex.Observable.combineLatest
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.BehaviorSubject
import java.util.*
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

    private val year = Calendar.getInstance().get(Calendar.YEAR)
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private val monthSubject = BehaviorSubject.create<Int>()
    var month = currentMonth
        set(monthValue) {
            field = monthValue
            monthSubject.onNext(monthValue)
        }

    private var bounds = LatLngBounds(LatLng(0.0, 0.0), LatLng(0.0, 0.0))
    private val boundsSubject = BehaviorSubject.create<LatLngBounds>()
    fun setBounds(boundsValue: LatLngBounds) {
        bounds = boundsValue
        boundsSubject.onNext(boundsValue)
    }

    private val forceLoadSubject = BehaviorSubject.create<Boolean>()
        .apply { onNext(true) }

    private val loadCrimeEventObservable =
        combineLatest(
            monthSubject,
            boundsSubject,
            forceLoadSubject,
            Function3 { _: Int, _: LatLngBounds, _: Boolean -> {} })
            .flatMap {
                _loadingEventsState.postValue(ViewState.Loading)
                repository.getCrimeEvents(
                    bounds.southwest.latitude, bounds.southwest.longitude,
                    bounds.northeast.latitude, bounds.northeast.longitude
                ).map { events ->
                    if (events.isNotEmpty()) {
                        ViewState.Content(events)
                    } else {
                        ViewState.Empty
                    }
                }
                    .onErrorReturn { error -> ViewState.Error(error) }
                    .toObservable()
            }

    private val disposeBag = CompositeDisposable()

    init {
        disposeBag += loadCrimeEventObservable
            .subscribe(
                { state ->
                    _loadingEventsState.postValue(state)
                },
                { error ->
                    _loadingEventsState.postValue(ViewState.Error(error))
                }
            )
    }

    override fun onCleared() {
        super.onCleared()
        disposeBag.clear()
    }

    fun loadCrimeEvents() = forceLoadSubject.onNext(true)
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