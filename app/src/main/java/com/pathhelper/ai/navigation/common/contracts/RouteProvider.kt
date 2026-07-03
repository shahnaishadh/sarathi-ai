package com.pathhelper.ai.navigation.common.contracts

import android.location.Location
/**
* Defines the contract for Route Provider operations.
*/
interface RouteProvider {
    fun calculateRoute(start: Location, destination: Location, callback: (List<Location>) -> Unit)
}
