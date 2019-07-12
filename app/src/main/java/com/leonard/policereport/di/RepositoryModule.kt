package com.leonard.policereport.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.leonard.policereport.repository.ApiService
import com.leonard.policereport.repository.BASE_URL
import com.leonard.policereport.repository.Repository
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

@Module
class RepositoryModule {
    @Provides
    fun provideGson(): Gson =
        GsonBuilder().create()

    @Provides
    fun providesRetrofit(gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

    @Provides
    fun providesRepository(apiService: ApiService) =
        Repository(apiService)
}