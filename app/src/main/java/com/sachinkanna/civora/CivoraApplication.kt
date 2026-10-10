package com.sachinkanna.civora

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CivoraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseEnvironment.configure(this)
        if (!FirebaseEnvironment.isDemo && FirebaseApp.getApps(this).isNotEmpty()) {
            com.google.firebase.appcheck.FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance())
        }
        ThemePreferences.load(this)
        DiagnosticsPreferences.load(this)
    }
}
object DiagnosticsPreferences {
    private val current = MutableStateFlow(false)
    val enabled = current.asStateFlow()
    fun load(context: Context) {
        val value = context.getSharedPreferences("civora_preferences",Context.MODE_PRIVATE).getBoolean("diagnostics",false)
        apply(value)
    }
    fun update(context: Context,value: Boolean) {
        context.getSharedPreferences("civora_preferences",Context.MODE_PRIVATE).edit().putBoolean("diagnostics",value).apply()
        apply(value)
    }
    private fun apply(value: Boolean) {
        current.value=value
        if (!FirebaseEnvironment.isDemo && runCatching { FirebaseApp.getInstance() }.isSuccess) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(value)
            if (!value) com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().deleteUnsentReports()
        }
    }
}
object FirebaseEnvironment {
    fun configure(context: Context) {
        val host=context.getString(R.string.firebase_emulator_host)
        val project=context.getString(R.string.firebase_emulator_project)
        val debug=(context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
        if(!debug||host.isBlank())return
        if(FirebaseApp.getApps(context).firstOrNull()?.options?.projectId==project)return
        FirebaseApp.getApps(context).firstOrNull()?.delete()
        FirebaseApp.initializeApp(context,FirebaseOptions.Builder().setProjectId(project).setStorageBucket("$project.appspot.com").setApplicationId("1:123456789:android:c1a0c1a0c1a0c1a0").setApiKey("AIza"+"0".repeat(35)).build())
        FirebaseAuth.getInstance().useEmulator(host,context.resources.getInteger(R.integer.firebase_emulator_auth_port))
        FirebaseFirestore.getInstance().useEmulator(host,context.resources.getInteger(R.integer.firebase_emulator_firestore_port))
        FirebaseFunctions.getInstance().useEmulator(host,context.resources.getInteger(R.integer.firebase_emulator_functions_port))
        FirebaseStorage.getInstance().useEmulator(host,context.resources.getInteger(R.integer.firebase_emulator_storage_port))
    }
    val isDemo: Boolean get()=runCatching { FirebaseApp.getInstance().options.projectId?.startsWith("demo-")==true }.getOrDefault(false)
}
object ThemePreferences {
    private val current=MutableStateFlow("SYSTEM")
    val mode=current.asStateFlow()
    fun load(context: Context) { current.value=context.getSharedPreferences("civora_preferences",Context.MODE_PRIVATE).getString("theme","SYSTEM")?:"SYSTEM" }
    fun update(context: Context,value: String) { if(value !in listOf("SYSTEM","LIGHT","DARK"))return;context.getSharedPreferences("civora_preferences",Context.MODE_PRIVATE).edit().putString("theme",value).apply();current.value=value }
}
object AppNavigation {
    val destination=MutableStateFlow<String?>(null)
    fun route(kind: String?,id: String?): String? {
        val entity=id.orEmpty();if(entity.isNotBlank()&&!entity.matches(Regex("[A-Za-z0-9_-]{1,257}")))return null
        return when(kind?.uppercase()) {"EVENT"->if(entity.isNotBlank())"Event:$entity"else"Events";"FOOD"->if(entity.isNotBlank())"Order:$entity"else"Food";"REPORT"->if(entity.isNotBlank())"Report:$entity"else"Report";"ANNOUNCEMENT"->if(entity.isNotBlank())"Notice:$entity"else"Notices";"TRANSPORT"->"Bus";"TIMETABLE"->"Timetable";else->null}
    }
}
