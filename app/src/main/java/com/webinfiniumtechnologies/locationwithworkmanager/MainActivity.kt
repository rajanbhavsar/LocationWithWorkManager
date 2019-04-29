package com.webinfiniumtechnologies.locationwithworkmanager

import android.Manifest
import android.location.Location
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.common.GoogleApiAvailability
import android.widget.Toast
import com.google.android.gms.location.LocationListener
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import androidx.work.*
import java.util.concurrent.TimeUnit
import androidx.work.WorkManager




class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener,
    GoogleApiClient.ConnectionCallbacks,LocationListener {


    private var location: Location? = null
    private var googleApiClient: GoogleApiClient? = null
    private val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
    private var locationRequest: LocationRequest? = null
    private val UPDATE_INTERVAL: Long = 5000
    private val FASTEST_INTERVAL: Long = 5000 // = 5 seconds
    // lists for permissions
    private var permissionsToRequest: ArrayList<String>? = null
    private val permissionsRejected = ArrayList<String>()
    private val permissions = ArrayList<String>()
    // integer for permissions results request
    private val ALL_PERMISSIONS_RESULT = 1011

    val LOCATION_WORK_TAG = "LOCATION_WORK_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionsToRequest = permissionsToRequest(permissions);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest!!.size > 0) {
                requestPermissions(
                    permissionsToRequest!!.toArray(
                    arrayOfNulls<String>(permissionsToRequest!!.size)), ALL_PERMISSIONS_RESULT);
            }else{
                // we build google api client
                googleApiClient = GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).build()
            }
        }else{
            // we build google api client
            googleApiClient = GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build()
        }




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
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        } else true

    }


    override fun onStart() {
        super.onStart()
        if(googleApiClient != null){
            googleApiClient!!.connect()
        }
    }

    override fun onResume() {
        super.onResume()
        if(!checkPlayServices()){
            locationMain.text = "You need to install Google Play Services to use the App properly"
        }
    }

    override fun onPause() {
        super.onPause()
        if (googleApiClient != null  &&  googleApiClient!!.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this)
            googleApiClient!!.disconnect();
        }


        callWorkManagerRequest();
    }

    private fun callWorkManagerRequest() {

        val constraints: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()


        val recurringWork: PeriodicWorkRequest = PeriodicWorkRequest.
            Builder(BackGroundLocationWorker::class.java, 16, TimeUnit.MINUTES).
            addTag(LOCATION_WORK_TAG).
            setConstraints(constraints).
            build()

        WorkManager.getInstance().enqueueUniquePeriodicWork(LOCATION_WORK_TAG,
            ExistingPeriodicWorkPolicy.REPLACE,recurringWork)
    }


    fun stopTrackLocation() {
        WorkManager.getInstance().cancelAllWorkByTag(LOCATION_WORK_TAG)
    }


    override fun onConnected(p0: Bundle?) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }


        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location != null) {
            locationMain.setText("Latitude : " + location!!.getLatitude() + "\nLongitude : " + location!!.getLongitude());
        }

        startLocationUpdates();
    }


    private fun startLocationUpdates() {
        locationRequest = LocationRequest()
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest!!.interval = UPDATE_INTERVAL
        locationRequest!!.fastestInterval = FASTEST_INTERVAL

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show()
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
    }

    override fun onConnectionSuspended(p0: Int) {

    }

    override fun onConnectionFailed(p0: ConnectionResult) {

    }





    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
            } else {
                finish()
            }

            return false
        }

        return true
    }


    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            locationMain.setText("Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude());
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            ALL_PERMISSIONS_RESULT -> {
                for (perm in permissions) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm)
                    }
                }

                if (permissionsRejected.size > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected[0])) {
                            AlertDialog.Builder(this@MainActivity)
                                .setMessage("These permissions are mandatory to get your location. You need to allow them.")
                                .setPositiveButton("OK",
                                    DialogInterface.OnClickListener { dialogInterface, i ->
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(
                                                permissionsRejected.toArray(
                                                    arrayOfNulls(
                                                        permissionsRejected.size
                                                    )
                                                ), ALL_PERMISSIONS_RESULT
                                            )
                                        }
                                    }).setNegativeButton("Cancel", null).create().show()

                            return
                        }
                    }
                } else {

                    // we build google api client
                    googleApiClient = GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this).build()

                    if (googleApiClient != null) {
                        googleApiClient!!.connect()
                    }
                }
            }
        }
    }
}
