package com.webinfiniumtechnologies.locationwithworkmanager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

/**
 * Created by Dipak.Vyas on 29/04/19.
 */


class BackGroundLocationWorker(appContext : Context,workparams : WorkerParameters) : Worker(appContext,workparams),GoogleApiClient.OnConnectionFailedListener,
    GoogleApiClient.ConnectionCallbacks, LocationListener {


    private var location: Location? = null
    private var googleApiClient: GoogleApiClient? = null
    private val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
    private var locationRequest: LocationRequest? = null
    private val UPDATE_INTERVAL: Long = 5000
    private val FASTEST_INTERVAL: Long = 5000 // = 5 seconds
    private val permissions = ArrayList<String>()
    private var permissionsToRequest: ArrayList<String>? = null


    override fun doWork(): Result {

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionsToRequest = permissionsToRequest(permissions);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest!!.size > 0) {

            }else{
                // we build google api client
                googleApiClient = GoogleApiClient.Builder(applicationContext).addApi(LocationServices.API).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).build()
                if(googleApiClient != null){
                    googleApiClient!!.connect()
                }
            }
        }else{
            // we build google api client
            googleApiClient = GoogleApiClient.Builder(applicationContext).addApi(LocationServices.API).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build()
            if(googleApiClient != null){
                googleApiClient!!.connect()
            }
        }
        return Result.success()
    }


    private fun permissionsToRequest(wantedPermissions: ArrayList<String>): ArrayList<String> {
        val result = ArrayList<String>()

        for (perm in wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm)
            }
        }

        return result
    }


    private fun hasPermission(permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            applicationContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        } else true

    }




    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    @SuppressLint("MissingPermission")
    override fun onConnected(p0: Bundle?) {

        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location != null) {
            Log.e("Location--","Latitude : " + location!!.getLatitude() + "\nLongitude : " + location!!.getLongitude());
        }

        startLocationUpdates();
    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            Log.e("Location--","Latitude : " + location!!.getLatitude() + "\nLongitude : " + location!!.getLongitude());
        }
    }


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationRequest = LocationRequest()
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest!!.interval = UPDATE_INTERVAL
        locationRequest!!.fastestInterval = FASTEST_INTERVAL
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
    }

}