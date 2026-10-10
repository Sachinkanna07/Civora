package com.sachinkanna.civora.data.datasource

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/** A single fresh fix per request. Callable writes never accumulate in an offline Firestore queue. */
class DriverLocationSource(context: Context,private val tripId: String,private val onState: (String)->Unit,private val onRevoked: ()->Unit={}) {
    private val context=context.applicationContext
    private val client=LocationServices.getFusedLocationProviderClient(context)
    private var lastAttempt=0L;private var inFlight=false;private var running=false;private var failures=0
    private val callback=object: LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location=result.lastLocation?:return
            val now=android.os.SystemClock.elapsedRealtime()
            val retryDelay=(15000L*(1L shl failures.coerceAtMost(3))).coerceAtMost(120000)
            if(!running||inFlight||now-lastAttempt<retryDelay)return
            val mock=androidx.core.location.LocationCompat.isMock(location)
            if(!location.hasAccuracy()||location.accuracy>200||location.accuracy<0||System.currentTimeMillis()-location.time>120000) {onState("Waiting for an accurate, fresh GPS fix");return}
            if(mock&&!com.sachinkanna.civora.FirebaseEnvironment.isDemo) {onState("Mock GPS is not allowed on the campus service");return}
            lastAttempt=now;inFlight=true
            onState("Sending current location?")
            FirebaseFunctions.getInstance().getHttpsCallable("publishTripLocation").call(mapOf("busId" to tripId,"latitude" to location.latitude,"longitude" to location.longitude,"accuracy" to location.accuracy.toDouble(),"capturedAt" to location.time,"locationSource" to if(mock)"SIMULATED"else"GPS"))
                .addOnSuccessListener {if(running) {failures=0;onState(if(mock)"Demo GPS shared (simulated)"else"GPS shared · updates every 15 seconds")}}
                .addOnFailureListener {e->if(running) {
                    failures=(failures+1).coerceAtMost(3)
                    if(e is FirebaseFunctionsException&&e.code==FirebaseFunctionsException.Code.PERMISSION_DENIED) {onState("Trip stopped or assignment changed");onRevoked()}
                    else onState("Location update interrupted · retrying with a fresh fix")
                }}.addOnCompleteListener {inFlight=false}
        }
    }
    fun start() {
        if(running)return
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {onState("Location permission needed");onRevoked();return}
        running=true;onState("Waiting for GPS")
        try {client.requestLocationUpdates(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,15000).setMinUpdateIntervalMillis(15000).build(),callback,Looper.getMainLooper()).addOnFailureListener {onState("Location service unavailable");onRevoked()}}
        catch(_: SecurityException) {onState("Location permission revoked");onRevoked()}
    }
    fun stop() {running=false;client.removeLocationUpdates(callback)}
}
