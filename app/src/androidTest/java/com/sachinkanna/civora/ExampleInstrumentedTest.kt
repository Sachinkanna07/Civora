package com.sachinkanna.civora

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun isolatedDemoUsesEmulatorProjectAndDisablesPushAutoInitialization() {
        org.junit.Assume.assumeTrue(FirebaseEnvironment.isDemo)
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(context.getString(R.string.firebase_emulator_project),com.google.firebase.FirebaseApp.getInstance().options.projectId)
        assertFalse(com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled)
    }
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.sachinkanna.civora", appContext.packageName)
    }
}
