package com.sachinkanna.civora

import org.junit.Assert.*
import org.junit.Test

class NotificationRoutingTest {
    @Test fun mealAndScheduleUpdatesOpenTheirActualModules() {
        assertEquals("Mess", AppNavigation.route("FOOD", ""))
        assertEquals("Order:order_123", AppNavigation.route("FOOD", "order_123"))
        assertEquals("Timetable", AppNavigation.route("TIMETABLE", ""))
        assertEquals("Report:report_123", AppNavigation.route("REPORT", "report_123"))
    }
    @Test fun rejectsMalformedExternalIdentifiersAndUnknownKinds() {
        assertNull(AppNavigation.route("REPORT", "../other"))
        assertNull(AppNavigation.route("EVENT", "https://example.com"))
        assertNull(AppNavigation.route("ADMIN", "anything"))
        assertNull(AppNavigation.route("EVENT", "a".repeat(258)))
    }
}
