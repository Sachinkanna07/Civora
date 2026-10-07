package com.sachinkanna.civora.ui.transport

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Devices without a map app can still open coordinates in their browser. */
fun openMap(context: Context, latitude: Double, longitude: Double, name: String = "") {
    val query = "$latitude,$longitude"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$query?q=$query(${Uri.encode(name)})")))
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Install a map app or browser to open directions.", Toast.LENGTH_LONG).show()
        }
    }
}
