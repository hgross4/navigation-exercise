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
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.util.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds


/**
 * Adapted from https://github.com/commonsguy/cw-omnibus/tree/master/MapsV2/Location
 */

class MainActivity : AbstractMapActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener,
        LocationSource, LocationListener, NavigationDialogFragment.DialogListener {

    private val TAG = MainActivity::class.java.getSimpleName()
    private var isInPermission = false
    private var mapLocationListener: LocationSource.OnLocationChangedListener? = null
    private var locMgr: LocationManager? = null
    private val crit = Criteria()
    private var needsInit = false
    private var map: GoogleMap? = null
    private var polyline: PolylineOptions = PolylineOptions()
    private var destination: Location = Location("")
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
            moveAndZoom(defaultLocation, DEFAULT_ZOOM)
        }

        map.setOnMapClickListener(this)
        map.isMyLocationEnabled = true
        locMgr = getSystemService(LOCATION_SERVICE) as LocationManager
        crit.accuracy = Criteria.ACCURACY_FINE

        buttonText = findViewById(R.id.navigation_button)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            if (!navigationUnderway) {
                startNavigation()
            } else {
                discontinueNavigation()
            }
        }

        polyline.width(5f)?.color(Color.BLUE)?.visible(true)?.zIndex(30f)

        getInitialLocation()
    }

    private fun moveAndZoom(latLng: LatLng, zoom: Float) {
        val center = CameraUpdateFactory.newLatLng(latLng)
        val zoom = CameraUpdateFactory.zoomTo(zoom)

        map?.moveCamera(center)
        map?.animateCamera(zoom)
    }

    private fun getInitialLocation() {

        var location: Location? = null

        if (canGetLocation()) {
            val criteria = Criteria()
            val provider = locMgr?.getBestProvider(criteria, true)
            try {
                location = locMgr?.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_LONG).show()
            }
        } else {
            return
        }

        if (location != null) {
            // Getting latitude of the current location
            val latitude = location.getLatitude()

            // Getting longitude of the current location
            val longitude = location.getLongitude()

            val currentPosition = LatLng(latitude, longitude)

            moveAndZoom(currentPosition, DEFAULT_ZOOM)
        }
    }

    private fun startNavigation() {
        navigationUnderway = true
        navigationStartTime = Calendar.getInstance().timeInMillis
        navigationDistance = 0f
        previousLocation = null
        buttonText?.setText(R.string.stop)
        follow(true)
    }

    private fun discontinueNavigation() {
        navigationUnderway = false
        buttonText?.setText(R.string.start)
        polyline = PolylineOptions()
        follow(false)
        map?.clear()
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
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION),
                    REQUEST_PERMS)
        }
    }

    private fun canGetLocation(): Boolean {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
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
                        NavigationDialogFragment.newInstance(elapsedTime.toString(),
                                navigationDistance.toString())
                dialogFragment.show(getSupportFragmentManager(), "")
                follow(false)
                showWholePath()
            }

            previousLocation = location

            val cu = CameraUpdateFactory.newLatLng(latlng)

            map?.addMarker(MarkerOptions().position(LatLng(destination.latitude, destination.longitude)))

            map?.animateCamera(cu)
        }
    }

    fun drawPath() {
        map?.clear()
        map?.addPolyline(polyline);
    }

    private fun showWholePath() {
        var hasPoints = false
        var maxLat: Double? = null
        var minLat: Double? = null
        var minLon: Double? = null
        var maxLon: Double? = null

        if (polyline.points != null) {
            val pts = polyline.points
            for (coordinate in pts) {
                // Find out the maximum and minimum latitudes & longitudes
                // Latitude
                maxLat = if (maxLat != null) Math.max(coordinate.latitude, maxLat) else coordinate.latitude
                minLat = if (minLat != null) Math.min(coordinate.latitude, minLat) else coordinate.latitude

                // Longitude
                maxLon = if (maxLon != null) Math.max(coordinate.longitude, maxLon) else coordinate.longitude
                minLon = if (minLon != null) Math.min(coordinate.longitude, minLon) else coordinate.longitude

                hasPoints = true
            }
        }

        if (hasPoints) {
            val builder = LatLngBounds.Builder()
            builder.include(LatLng(maxLat!!, maxLon!!))
            builder.include(LatLng(minLat!!, minLon!!))
            map?.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 48))
        }
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

    override fun onDialogDismissed() {
        discontinueNavigation()
    }

    companion object {
        private val STATE_IN_PERMISSION = "inPermission"
        private val REQUEST_PERMS = 1337
    }
}
