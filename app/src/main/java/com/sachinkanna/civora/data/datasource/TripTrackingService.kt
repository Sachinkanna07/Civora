package com.sachinkanna.civora.data.datasource

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.content.pm.PackageManager
import android.Manifest
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestoreException
import com.sachinkanna.civora.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow

class TripTrackingService: Service() {
    private var source: DriverLocationSource?=null
    private var listener: ListenerRegistration?=null
    private var busListener: ListenerRegistration?=null
    private var authListener: FirebaseAuth.AuthStateListener?=null
    private var tripId=""
    override fun onBind(intent: Intent?): IBinder?=null
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        if(intent?.action==STOP) {stopSelf();return START_NOT_STICKY}
        val id=intent?.getStringExtra("tripId").orEmpty()
        val uid=FirebaseAuth.getInstance().currentUser?.uid
        if(id.isBlank()||uid==null) {stopSelf();return START_NOT_STICKY}
        if(!canShareLocation(this)) {status.value="Location and trip notification access are required";stopSelf();return START_NOT_STICKY}
        try {ServiceCompat.startForeground(this,NOTIFICATION,notification("Waiting for GPS"),ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)}
        catch(_: Exception) {status.value="Location permission or foreground start unavailable";stopSelf();return START_NOT_STICKY}
        if(tripId==id&&source!=null)return START_NOT_STICKY
        cleanup();tripId=id;activeTrip.value=id
        val auth=FirebaseAuth.getInstance()
        authListener=FirebaseAuth.AuthStateListener {if(it.currentUser?.uid!=uid)stopSelf()}.also {auth.addAuthStateListener(it)}
        val db=FirebaseFirestore.getInstance()
        listener=db.collection("activeTrips").document(id).addSnapshotListener {snap,e->
            if(e?.code in setOf(FirebaseFirestoreException.Code.PERMISSION_DENIED,FirebaseFirestoreException.Code.UNAUTHENTICATED)) {stopSelf();return@addSnapshotListener}
            if(e!=null||snap==null) {update("Trip validation interrupted · retrying");return@addSnapshotListener}
            if(!snap.exists()||snap.getBoolean("active")!=true||snap.getString("driverId")!=uid) {stopSelf();return@addSnapshotListener}
            if(source==null&&!snap.metadata.isFromCache) {source=DriverLocationSource(this,id,::update,{stopSelf()}).also {it.start()}}
        }
        busListener=db.collection("buses").document(id).addSnapshotListener {snap,_->if(snap!=null&&(!snap.exists()||snap.getString("driverId")!=uid))stopSelf()}
        return START_NOT_STICKY
    }
    private fun update(value: String) {status.value=value;getSystemService(NotificationManager::class.java).notify(NOTIFICATION,notification(value))}
    private fun notification(text: String): Notification {
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,"Active trip location",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop=PendingIntent.getService(this,1,Intent(this,TripTrackingService::class.java).setAction(STOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("Civora · trip location sharing").setContentText(text).setContentIntent(open).setOngoing(true).addAction(android.R.drawable.ic_media_pause,"Stop sharing",stop).build()
    }
    private fun cleanup() {source?.stop();source=null;listener?.remove();listener=null;busListener?.remove();busListener=null;authListener?.let {FirebaseAuth.getInstance().removeAuthStateListener(it)};authListener=null}
    override fun onDestroy() {cleanup();activeTrip.value=null;status.value="Location sharing stopped";ServiceCompat.stopForeground(this,ServiceCompat.STOP_FOREGROUND_REMOVE);super.onDestroy()}
    companion object {
        private const val CHANNEL="civora_trip_location";private const val NOTIFICATION=7001;private const val STOP="civora.STOP_LOCATION"
        val status=MutableStateFlow("Location sharing stopped")
        val activeTrip=MutableStateFlow<String?>(null)
        fun canShareLocation(context: Context): Boolean {
            val located=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
            val manager=context.getSystemService(NotificationManager::class.java)?:return false
            return located && manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL)?.importance!=NotificationManager.IMPORTANCE_NONE
        }
        fun start(context: Context,id: String) {ContextCompat.startForegroundService(context,Intent(context,TripTrackingService::class.java).putExtra("tripId",id))}
        fun stop(context: Context) {context.stopService(Intent(context,TripTrackingService::class.java))}
    }
}
