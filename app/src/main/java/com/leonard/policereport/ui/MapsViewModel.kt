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
import retrofit2.HttpException
import javax.inject.Inject

class MapsViewModel(private val repository: Repository) : ViewModel() {
    sealed class ViewState {
        object Loading : ViewState()
        class GenericError(val exception: Throwable) : ViewState()
        object TooManyEvents : ViewState()
        object Empty : ViewState()
        data class Content(val events: List<CrimeEvent>) : ViewState()
    }

    private val _loadingEventsState = MutableLiveData<ViewState>().apply { value = ViewState.Loading }
    val loadingEventsState: LiveData<ViewState> = _loadingEventsState

    //Latitude and Longitude of London City
    var location = LatLng(51.5131808, -0.090536)
    var zoom = 18f

    //hardcoded to 2019
    private val year = 2019
    private val monthSubject = BehaviorSubject.create<Int>()
    //hard coded to May
    var month = 5
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
            .switchMap {
                _loadingEventsState.postValue(ViewState.Loading)
                repository.getCrimeEvents(
                    bounds.southwest.latitude, bounds.southwest.longitude,
                    bounds.northeast.latitude, bounds.northeast.longitude,
                    year, month
                )
                    .map { events ->
                        when (events.size) {
                            0 -> ViewState.Empty
                            in 1..2000 -> ViewState.Content(events)
                            else -> ViewState.TooManyEvents
                        }
                    }
                    .onErrorReturn { error ->
                        if (error is HttpException && error.code() == 503) {
                            ViewState.TooManyEvents
                        } else {
                            ViewState.GenericError(error)
                        }

                    }
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
                    _loadingEventsState.postValue(ViewState.GenericError(error))
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