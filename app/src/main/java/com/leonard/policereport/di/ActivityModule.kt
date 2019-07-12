package com.leonard.policereport.di

import com.leonard.policereport.ui.MapsActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityModule {
    @ContributesAndroidInjector
    abstract fun contributeMainActivity(): MapsActivity
}