package com.leonard.policereport.repository

import com.leonard.policereport.model.CrimeEvent
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class Repository(private val apiService: ApiService) {
    fun getCrimeEvents(
        southWestLat: Float,
        southWestLong: Float,
        northEastLat: Float,
        northEastlong: Float
    ): Single<List<CrimeEvent>> {
        val poly =
            "$northEastLat,$southWestLong:$northEastLat,$northEastlong:$southWestLat,$northEastlong:$southWestLat,$southWestLong"
        return apiService.getCrimeEvents(poly)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
    }
}