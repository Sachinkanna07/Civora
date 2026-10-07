package com.sachinkanna.civora.data.model

import kotlin.math.*

data class Bus(
    val id: String = "",
    val name: String = "",
    val routeId: String = "",
    val driverId: String = "",
)

data class Driver(val uid: String = "", val busId: String = "")

data class BusStop(val name: String = "", val latitude: Double = 0.0, val longitude: Double = 0.0)

data class BusRoute(
    val id: String = "",
    val name: String = "",
    val stops: List<BusStop> = emptyList(),
)

data class BusLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val updatedAt: Long = 0,
)

data class ActiveTrip(
    val id: String = "",
    val busId: String = "",
    val routeId: String = "",
    val driverId: String = "",
    val active: Boolean = false,
    val location: BusLocation? = null,
)

fun isLocationFresh(location: BusLocation?, now: Long): Boolean =
    location != null && now - location.updatedAt in 0..120_000

fun etaMinutes(location: BusLocation?, stop: BusStop, now: Long): Int? {
    if (!isLocationFresh(location, now)) return null
    location ?: return null
    val lat = Math.toRadians(stop.latitude - location.latitude)
    val lng = Math.toRadians(stop.longitude - location.longitude)
    val a =
        sin(lat / 2).pow(2) +
            cos(Math.toRadians(location.latitude)) *
                cos(Math.toRadians(stop.latitude)) *
                sin(lng / 2).pow(2)
    val km = 6371 * 2 * atan2(sqrt(a), sqrt((1 - a).coerceAtLeast(0.0)))
    return ceil(km / 20 * 60).toInt().coerceAtLeast(1)
}
