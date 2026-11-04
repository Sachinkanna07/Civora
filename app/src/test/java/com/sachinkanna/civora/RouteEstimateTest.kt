package com.sachinkanna.civora

import com.sachinkanna.civora.data.model.*
import org.junit.Assert.*
import org.junit.Test

class RouteEstimateTest {
    private val start=BusStop("Start",0.0,0.0)
    private val bend=BusStop("Bend",0.0,0.1)
    private val end=BusStop("End",0.1,0.1)
    private val route=BusRoute("route","Test route",listOf(start,bend,end))
    @Test fun followsInterveningStopsInsteadOfStraightLineToDestination() {
        val location=BusLocation(0.0,0.0,1000)
        assertTrue(routeEtaMinutes(location,route,end,1000)!! > etaMinutes(location,end,1000)!!)
        assertTrue(routeEtaMinutes(location,route,end,1000)!! > routeEtaMinutes(location,route,bend,1000)!!)
    }
    @Test fun refusesStaleOffRouteAndPassedStops() {
        assertNull(routeEtaMinutes(BusLocation(0.0,0.0,1),route,end,200000))
        assertNull(routeEtaMinutes(BusLocation(2.0,2.0,1000),route,end,1000))
        assertNull(routeEtaMinutes(BusLocation(0.05,0.1,1000),route,start,1000))
        assertNull(routeEtaMinutes(BusLocation(0.05,0.1,1000),route,bend,1000))
    }
}
