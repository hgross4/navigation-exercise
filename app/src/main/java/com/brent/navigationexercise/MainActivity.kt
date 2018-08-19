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
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions




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
    private var autoFollow = true
    var polyline: PolylineOptions = PolylineOptions()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        if (state == null) {
            needsInit = true
        } else {
            isInPermission = state.getBoolean(STATE_IN_PERMISSION, false)
            autoFollow = state.getBoolean(STATE_AUTO_FOLLOW, true)
        }
        onCreateForRealz(canGetLocation())
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        this.map = map

        if (needsInit) {
            val center = CameraUpdateFactory.newLatLng(LatLng(40.76793169992044,
                    -73.98180484771729))
            val zoom = CameraUpdateFactory.zoomTo(15f)

            map.moveCamera(center)
            map.animateCamera(zoom)
        }

        map.setOnMapClickListener(this)
        map.isMyLocationEnabled = true
        locMgr = getSystemService(LOCATION_SERVICE) as LocationManager
        crit.accuracy = Criteria.ACCURACY_FINE

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            follow()
        }
        polyline.width(5f)?.color(Color.BLUE)?.visible(true)?.zIndex(30f)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(STATE_IN_PERMISSION, isInPermission)
        outState.putBoolean(STATE_AUTO_FOLLOW, autoFollow)
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

        // todo remove the line below or modify it for correct behavior on app start up
//        follow()
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

            polyline.add(latlng)
            drawPath()

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
    private fun follow() {
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
        map!!.clear()
        map!!.addMarker(MarkerOptions().position(point!!))
    }

    companion object {
        private val STATE_IN_PERMISSION = "inPermission"
        private val STATE_AUTO_FOLLOW = "autoFollow"
        private val REQUEST_PERMS = 1337
    }
}
