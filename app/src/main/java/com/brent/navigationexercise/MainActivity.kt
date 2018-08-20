package com.brent.navigationexercise

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.TextView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.util.*


/**
 * Adapted from https://github.com/commonsguy/cw-omnibus/tree/master/MapsV2/Location
 */

class MainActivity : AbstractMapActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener, LocationSource, LocationListener {

    private val TAG = MainActivity::class.java.getSimpleName()
    private var isInPermission = false
    private var mapLocationListener: LocationSource.OnLocationChangedListener? = null
    private var locMgr: LocationManager? = null
    private val crit = Criteria()
    private var needsInit = false
    private var map: GoogleMap? = null
    var polyline: PolylineOptions = PolylineOptions()
    var destination: Location = Location("")
    private val defaultLocation = LatLng(41.8781, -87.6298) // Chicago
    private val DEFAULT_ZOOM = 15f
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var navigationUnderway: Boolean = false
    private var navigationStartTime = 0L
    private var navigationStopTime = 0L
    private var navigationDistance = 0f
    private var previousLocation: Location? = null
    private var buttonText: TextView? = null

    // maximum distance in meters from destination in order to declare arrival
    private val ARRIVAL_THRESHOLD = 10

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        if (state == null) {
            needsInit = true
        } else {
            isInPermission = state.getBoolean(STATE_IN_PERMISSION, false)
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        onCreateForRealz(canGetLocation())
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        this.map = map

        if (needsInit) {
            val center = CameraUpdateFactory.newLatLng(defaultLocation)
            val zoom = CameraUpdateFactory.zoomTo(DEFAULT_ZOOM)

            map.moveCamera(center)
            map.animateCamera(zoom)
        }

        map.setOnMapClickListener(this)
        map.isMyLocationEnabled = true
        locMgr = getSystemService(LOCATION_SERVICE) as LocationManager
        crit.accuracy = Criteria.ACCURACY_FINE

        buttonText = findViewById(R.id.navigation_button)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            if (!navigationUnderway) {
                initiateNavigation()
            } else {
                discontinueNavigation()
            }
        }
        polyline.width(5f)?.color(Color.BLUE)?.visible(true)?.zIndex(30f)
    }

    fun initiateNavigation() {
        navigationUnderway = true
        navigationStartTime = Calendar.getInstance().timeInMillis
        navigationDistance = 0f
        previousLocation = null
        buttonText?.setText(R.string.stop)
    }

    fun discontinueNavigation() {
        navigationUnderway = false
        buttonText?.setText(R.string.start)
        polyline = PolylineOptions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(STATE_IN_PERMISSION, isInPermission)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        isInPermission = false

        if (requestCode == REQUEST_PERMS) {
            if (canGetLocation()) {
                onCreateForRealz(true)
            } else {
                finish() // denied permission, so we're done
            }
        }
    }

    private fun onCreateForRealz(canGetLocation: Boolean) {
        if (canGetLocation) {
            if (readyToGo()) {
                setContentView(R.layout.activity_main)

                val mapFrag = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

                mapFrag.getMapAsync(this)
            }
        } else if (!isInPermission) {
            isInPermission = true

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMS)
        }
    }

    private fun canGetLocation(): Boolean {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onStart() {
        super.onStart()

        follow(true)
    }

    override fun onStop() {
        map?.setLocationSource(null)
        locMgr?.removeUpdates(this)

        super.onStop()
    }

    override fun activate(listener: LocationSource.OnLocationChangedListener) {
        this.mapLocationListener = listener
    }

    override fun deactivate() {
        this.mapLocationListener = null
    }

    override fun onLocationChanged(location: Location) {
        if (mapLocationListener != null) {

            mapLocationListener!!.onLocationChanged(location)

            val latlng = LatLng(location.latitude, location.longitude)

            if (navigationUnderway) {

                if (previousLocation != null) {
                    navigationDistance += location.distanceTo(previousLocation)
                }

                polyline.add(latlng)

                drawPath()

                if (location.distanceTo(destination) < ARRIVAL_THRESHOLD) {
                    navigationUnderway = false
                    navigationStopTime = Calendar.getInstance().timeInMillis
                    val elapsedTime = navigationStopTime - navigationStartTime
                    val dialogFragment =
                            NavigationDialogFragment.newInstance(elapsedTime.toString(), navigationDistance.toString())
                    dialogFragment.show(getSupportFragmentManager(), "")
                }

                previousLocation = location
            }

            val cu = CameraUpdateFactory.newLatLng(latlng)

            map!!.animateCamera(cu)
        }
    }

    fun drawPath() {
        map?.clear()
        map?.addPolyline(polyline);
    }

    override fun onProviderDisabled(provider: String) {
        // unused
    }

    override fun onProviderEnabled(provider: String) {
        // unused
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        // unused
    }

    @SuppressLint("MissingPermission")
    private fun follow(autoFollow: Boolean) {
        if (map != null && locMgr != null) {
            if (autoFollow) {
                locMgr!!.requestLocationUpdates(0L, 0.0f, crit, this, null)
                map!!.setLocationSource(this)
                map!!.uiSettings.isMyLocationButtonEnabled = false
            } else {
                map!!.uiSettings.isMyLocationButtonEnabled = true
                map!!.setLocationSource(null)
                locMgr!!.removeUpdates(this)
            }
        }
    }

    override fun onMapClick(point: LatLng?) {
        if (!navigationUnderway) {
            map!!.clear()
            map!!.addMarker(MarkerOptions().position(point!!))
            destination.latitude = point.latitude
            destination.longitude = point.longitude
        }
    }

    companion object {
        private val STATE_IN_PERMISSION = "inPermission"
        private val REQUEST_PERMS = 1337
    }
}
