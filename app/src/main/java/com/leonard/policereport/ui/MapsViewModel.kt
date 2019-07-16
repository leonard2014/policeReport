package com.leonard.policereport.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.leonard.policereport.model.Location
import com.leonard.policereport.repository.Repository
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.rxkotlin.plusAssign
import retrofit2.HttpException
import javax.inject.Inject

class MapsViewModel(private val repository: Repository) : ViewModel() {
    sealed class ViewState {
        object Idle: ViewState()
        object Loading : ViewState()
        class GenericError(val exception: Throwable) : ViewState()
        object TooManyEvents : ViewState()
        object Empty : ViewState()
        data class Content(val events: List<Location>) : ViewState()
    }

    private val _loadingEventsState = MutableLiveData<ViewState>().apply { value = ViewState.Idle }
    val loadingEventsState: LiveData<ViewState> = _loadingEventsState

    //Latitude and Longitude of London City
    var location = LatLng(51.5131808, -0.090536)
    var zoom = 18f

    //hardcoded to 2019
    private val year = 2019
    private val monthStream = BehaviorProcessor.create<Int>().apply { onBackpressureLatest() }
    //hard coded to May
    var month = 5
        set(monthValue) {
            field = monthValue
            monthStream.onNext(monthValue)
        }

    private val boundsStream = BehaviorProcessor.create<LatLngBounds>().apply { onBackpressureLatest() }
    fun setBounds(boundsValue: LatLngBounds) {
        boundsStream.onNext(boundsValue)
    }

    private val forceLoadStream = BehaviorProcessor.create<Boolean>().apply { onBackpressureLatest() }
        .apply { onNext(true) }

    private val loadCrimeEventObservable =
        Flowable.combineLatest(
            monthStream, boundsStream, forceLoadStream,
            Function3<Int, LatLngBounds, Boolean, Pair<Int, LatLngBounds>> { _month, _bounds, _ -> _month to _bounds }
        )
            .switchMap { (_month, _bounds) ->
                _loadingEventsState.postValue(ViewState.Loading)
                repository.getCrimeEventsLocation(
                    _bounds.southwest.latitude, _bounds.southwest.longitude,
                    _bounds.northeast.latitude, _bounds.northeast.longitude,
                    year, _month
                )
                    .map { events ->
                        when (events.size) {
                            0 -> ViewState.Empty
                            in 1..1000 -> ViewState.Content(events)
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
                    .toFlowable()
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

    fun loadCrimeEvents() = forceLoadStream.onNext(true)
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