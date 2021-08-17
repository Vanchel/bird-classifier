package com.vanchel.birdclassifier.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {
    private val _isPermissionGranted = MutableLiveData<Boolean>()
    val isPermissionGranted: LiveData<Boolean>
        get() = _isPermissionGranted

    fun onPermissionGranted() {
        _isPermissionGranted.value = true
    }

    fun onPermissionDenied() {
        _isPermissionGranted.value = false
    }
}