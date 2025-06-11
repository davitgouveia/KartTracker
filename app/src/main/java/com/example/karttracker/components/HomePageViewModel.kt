package com.example.karttracker.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.karttracker.data.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class HomePageViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val locationHelper = LocationHelper(application.applicationContext)
    val currentCity = locationHelper.currentCity

    init {
        // Fetch the city when the ViewModel is created
        viewModelScope.launch {
            locationHelper.fetchCurrentCity()
        }
    }
}