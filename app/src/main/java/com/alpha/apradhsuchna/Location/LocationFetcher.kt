package com.alpha.apradhsuchna.Location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.alpha.apradhsuchna.Dashboard
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.util.*

class LocationFetcher(private val context: Context) : LocationListener {

    private var locationManager: LocationManager? = null
    lateinit var rootRef: CollectionReference

    //interface
    interface OnLocationFetchedListener {
        fun onLocationFetched(address: String)
    }

    private var onLocationFetchedListener: OnLocationFetchedListener? = null

    fun setOnLocationFetchedListener(listener: OnLocationFetchedListener) {
        onLocationFetchedListener = listener
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
    }


    private fun getAddress(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(context, Locale("hi", "IN"))
        var address = ""
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val returnedAddress = addresses[0]
                val sb = StringBuilder("")
                for (i in 0..returnedAddress.maxAddressLineIndex) {
                    sb.append(returnedAddress.getAddressLine(i)).append(", ")
                }
                address = sb.toString().trim()
                onLocationFetchedListener?.onLocationFetched(address)
                Log.e("Address",address)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        // Use the address here
    }

    override fun onLocationChanged(location: Location) {
        Log.e("Address","${location.longitude}")
        getAddress(location.latitude, location.longitude)
        // Stop listening for location updates
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(this)
    }
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }
}
