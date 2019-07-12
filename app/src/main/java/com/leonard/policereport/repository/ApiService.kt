package com.leonard.policereport.repository

import com.leonard.policereport.model.CrimeEvent
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api/crimes-street/all-crime")
    fun getCrimeEvents(@Query("poly") poly: String): Single<List<CrimeEvent>>
}