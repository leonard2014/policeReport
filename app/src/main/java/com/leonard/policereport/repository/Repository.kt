package com.leonard.policereport.repository

import com.leonard.policereport.model.Location
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class Repository(private val apiService: ApiService) {
    fun getCrimeEventsLocation(
        southWestLat: Double,
        southWestLong: Double,
        northEastLat: Double,
        northEastLong: Double,
        year: Int,
        month: Int
    ): Single<List<Location>> {
        val poly =
            "$northEastLat,$southWestLong:$northEastLat,$northEastLong:$southWestLat,$northEastLong:$southWestLat,$southWestLong"
        val date = String.format("%04d-%02d", year, month)
        return apiService.getCrimeEvents(poly, date)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .map { events ->
                events
                    .groupBy { event -> event.location }
                    .keys.toList()
            }
    }
}