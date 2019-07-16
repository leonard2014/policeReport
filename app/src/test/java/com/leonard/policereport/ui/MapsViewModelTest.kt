package com.leonard.policereport.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.leonard.policereport.model.CrimeEvent
import com.leonard.policereport.model.Location
import com.leonard.policereport.model.Street
import com.leonard.policereport.repository.Repository
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import okhttp3.ResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before

import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class MapsViewModelTest {
    @get:Rule
    val mockitoRule = MockitoJUnit.rule()

    @get:Rule
    val taskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var repository: Repository

    private lateinit var viewModel: MapsViewModel

    @Before
    fun setUp() {
        viewModel = MapsViewModel(repository)
    }

    @Test
    fun `when loading succeeds should send loading then content state`() {
        val location = Location(23.5, 35.8, Street(1, "Liverpool"))
        val matches = arrayListOf(
            CrimeEvent("1", "2", 1, location, "subtype", "loc_type", "5", "pid"),
            CrimeEvent("9", "8", 7, location, "subtype", "loc_type", "5", "pid")
        )

        whenever(repository.getCrimeEvents(any(), any(), any(), any(), any(), any())).thenReturn(Single.just(matches))
        val testObserver = viewModel.loadingEventsState.testObserver()

        viewModel.month = 9
        viewModel.setBounds(LatLngBounds(LatLng(39.2, 0.9), LatLng(40.5, 0.3)))

        assertThat(testObserver.observedValues.size).isEqualTo(3)
        assertThat(testObserver.observedValues[0]).isEqualTo(MapsViewModel.ViewState.Loading)
        assertThat(testObserver.observedValues[1]).isEqualTo(MapsViewModel.ViewState.Loading)
        assertThat(testObserver.observedValues[2]).isEqualTo(MapsViewModel.ViewState.Content(matches))
    }

    @Test
    fun `when loading fails with generic error should send generic error state`() {
        val error = IOException()

        whenever(repository.getCrimeEvents(any(), any(), any(), any(), any(), any())).thenReturn(Single.error(error))
        val testObserver = viewModel.loadingEventsState.testObserver()

        viewModel.month = 9
        viewModel.setBounds(LatLngBounds(LatLng(39.2, 0.9), LatLng(40.5, 0.3)))

        assertThat(testObserver.observedValues.size).isEqualTo(3)
        assertThat(testObserver.observedValues[0]).isEqualTo(MapsViewModel.ViewState.Loading)
        assertThat(testObserver.observedValues[1]).isEqualTo(MapsViewModel.ViewState.Loading)
        assertThat((testObserver.observedValues[2] as MapsViewModel.ViewState.GenericError).exception).isEqualTo(error)
    }

    @Test
    fun `when loading fails with 503 error should send too many events error state`() {
        val error = HttpException(Response.error<Int>(503, ResponseBody.create(null, "error")))

        whenever(repository.getCrimeEvents(any(), any(), any(), any(), any(), any())).thenReturn(Single.error(error))
        val testObserver = viewModel.loadingEventsState.testObserver()

        viewModel.month = 9
        viewModel.setBounds(LatLngBounds(LatLng(39.2, 0.9), LatLng(40.5, 0.3)))

        assertThat(testObserver.observedValues.size).isEqualTo(3)
        assertThat(testObserver.observedValues[0]).isEqualTo(MapsViewModel.ViewState.Loading)
        assertThat(testObserver.observedValues[1]).isEqualTo(MapsViewModel.ViewState.Loading)
        assertThat(testObserver.observedValues[2]).isEqualTo(MapsViewModel.ViewState.TooManyEvents)
    }

    @Test
    fun `when getting empty content should return empty state`() {
        val emptyList = emptyList<CrimeEvent>()

        whenever(repository.getCrimeEvents(any(), any(), any(), any(), any(), any())).thenReturn(Single.just(emptyList))
        val testObserver = viewModel.loadingEventsState.testObserver()

        viewModel.month = 9
        viewModel.setBounds(LatLngBounds(LatLng(39.2, 0.9), LatLng(40.5, 0.3)))

        assertThat(testObserver.observedValues.size).isEqualTo(3)
        assertThat(testObserver.observedValues[0]).isEqualTo(MapsViewModel.ViewState.Loading)
        assertThat(testObserver.observedValues[1]).isEqualTo(MapsViewModel.ViewState.Loading)
        assertThat(testObserver.observedValues[2]).isEqualTo(MapsViewModel.ViewState.Empty)
    }
}