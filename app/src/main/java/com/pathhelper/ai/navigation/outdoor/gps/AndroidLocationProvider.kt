package com.pathhelper.ai.navigation.outdoor.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.pathhelper.ai.navigation.common.contracts.LocationProvider
/**
* Coordinates Android Location Provider operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Android Location Provider.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class AndroidLocationProvider(private val context: Context) : LocationProvider {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var callback: ((Location) -> Unit)? = null

    private val locationListener =
object : LocationListener {
        override fun onLocationChanged(location: Location) {
            callback?.invoke(location)
        }
        @Deprecated("Deprecated in API 30")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    override fun startTracking(callback: (Location) -> Unit) {
        this.callback = callback
        try {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (isGpsEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    locationListener
                )
            } else if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    1f,
                    locationListener
                )
            }
        } catch (e: Exception) {
            // Suppress tracking setup exceptions in unprivileged runtime
        }
    }

    override fun stopTracking() {
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            // Suppress removal exceptions
        }
        this.callback = null
    }

    @SuppressLint("MissingPermission")
    override fun getLastKnownLocation(): Location? {
        return try {
            val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            gpsLoc ?: netLoc
        } catch (e: Exception) {
            null
        }
    }
}
