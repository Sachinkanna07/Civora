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
    val contactPhone: String = "",
)

data class BusLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val updatedAt: Long = 0,
    val simulated: Boolean = false,
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

/** Coarse estimate along the ordered stop polyline, with no traffic claim.
 * A stop behind the nearest segment or GPS far from the route has no estimate.
 */
fun routeEtaMinutes(location: BusLocation?, route: BusRoute?, stop: BusStop, now: Long): Int? {
    if (!isLocationFresh(location, now) || location == null || route == null || route.stops.size < 2) return null
    val destination = route.stops.indexOf(stop)
    if (destination < 0) return null
    val scale = cos(Math.toRadians(location.latitude)).coerceAtLeast(0.01)
    fun point(value: BusStop): Pair<Double,Double> =
        (value.longitude-location.longitude)*111.32*scale to (value.latitude-location.latitude)*111.32
    data class Segment(val index: Int,val fraction: Double,val distance: Double,val length: Double)
    val closest = route.stops.zipWithNext().mapIndexed { index,(start,end) ->
        val (ax,ay)=point(start); val (bx,by)=point(end);val dx=bx-ax;val dy=by-ay
        val squared=dx*dx+dy*dy
        val fraction=if(squared==0.0)0.0 else (-(ax*dx+ay*dy)/squared).coerceIn(0.0,1.0)
        Segment(index,fraction,hypot(ax+fraction*dx,ay+fraction*dy),sqrt(squared))
    }.minByOrNull { it.distance } ?: return null
    if (closest.distance > 2.0 || destination <= closest.index && closest.fraction > 0.05 || destination < closest.index) return null
    var distance=if(destination==closest.index)0.0 else closest.length*(1.0-closest.fraction)
    for(index in closest.index+1 until destination) {
        val (ax,ay)=point(route.stops[index]);val (bx,by)=point(route.stops[index+1]);distance+=hypot(bx-ax,by-ay)
    }
    val intermediateStops=(destination-closest.index-1).coerceAtLeast(0)
    return (ceil(distance/20*60).toInt()+intermediateStops).coerceAtLeast(1)
}
