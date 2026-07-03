package com.pathhelper.ai.navigation.common.contracts

import android.location.Location
/**
* Defines the contract for Location Provider operations.
*/
interface LocationProvider {
    fun startTracking(callback: (Location) -> Unit)
    fun stopTracking()
    fun getLastKnownLocation(): Location?
}
