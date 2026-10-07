package com.sachinkanna.civora

import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.viewmodel.*
import com.sachinkanna.civora.ui.activity.destinationRoute
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class CampusModuleStateTest {
    @Test
    fun publicRegistrationIsStudentOnlyAndValidatesInput() {
        assertEquals(
            UserRole.STUDENT,
            validateStudentRegistration("Campus User", "user@campus.edu", "password1"),
        )
        for (fields in
            listOf(
                Triple("x", "user@campus.edu", "password1"),
                Triple("Campus User", "bad", "password1"),
                Triple("Campus User", "user@campus.edu", "short"),
            )) {
            try {
                validateStudentRegistration(fields.first, fields.second, fields.third)
                fail("Invalid registration accepted")
            } catch (expected: IllegalArgumentException) {}
        }
    }

    @Test
    fun roleParsingFailsClosed() {
        assertEquals(UserRole.ADMIN, UserRole.fromKey("ADMIN"))
        assertNull(UserRole.fromKey("superuser"))
        assertNull(UserRole.fromKey(null))
    }

    @Test
    fun announcementDepartmentIsExactAndExpiredNoticesHidden() {
        val items =
            listOf(
                Announcement(title = "CSE", department = "CSE"),
                Announcement(title = "ECE", department = "ECE"),
                Announcement(title = "General"),
                Announcement(title = "Expired", department = "CSE", expiresAt = 1),
            )
        assertEquals(
            listOf("CSE"),
            AnnouncementUiState(items = items, department = "CSE", departmentOnly = true)
                .filtered
                .map { it.title },
        )
        assertTrue(AnnouncementUiState(items = items, departmentOnly = true).filtered.isEmpty())
    }

    @Test
    fun eventFilteringKeepsCurrentEventsButHidesEndedEvents() {
        val now = System.currentTimeMillis()
        val state =
            EventsUiState(
                items =
                    listOf(
                        CampusEvent(
                            title = "Live",
                            category = "Workshop",
                            startTime = now - 1000,
                            endTime = now + 60000,
                        ),
                        CampusEvent(title = "Past", category = "Workshop", endTime = now - 1),
                    )
            )
        assertEquals("Live", state.copy(selectedCategory = "Workshop").filtered.single().title)
    }

    @Test
    fun timetableFindsNextClassAndRollsToNextWeek() {
        val monday = LocalDateTime.of(2026, 10, 5, 8, 0)
        val entries =
            listOf(
                TimetableEntry(
                    id = "a",
                    courseName = "Algorithms",
                    dayOfWeek = 1,
                    startMinute = 540,
                    endMinute = 600,
                ),
                TimetableEntry(id = "b", dayOfWeek = 1, startMinute = 660, endMinute = 720),
            )
        assertEquals("a", nextClass(entries, monday)?.entry?.id)
        assertEquals("b", nextClass(entries, monday.withHour(10))?.entry?.id)
        assertEquals(
            monday.toLocalDate().plusWeeks(1),
            nextClass(entries, monday.withHour(13))?.date?.toLocalDate(),
        )
        assertEquals(listOf(600 until 660), freePeriods(entries))
        assertNull(nextClass(emptyList(), monday))
    }

    @Test
    fun overlappingClassesDoNotCreateFalseFreePeriods() {
        val schedule = listOf(
            TimetableEntry(startMinute = 540, endMinute = 720),
            TimetableEntry(startMinute = 600, endMinute = 660),
            TimetableEntry(startMinute = 690, endMinute = 750),
            TimetableEntry(startMinute = 780, endMinute = 840),
        )
        assertEquals(listOf(750 until 780), freePeriods(schedule))
    }

    @Test
    fun invalidTimetableTimesAreIgnored() {
        assertNull(nextClass(listOf(TimetableEntry(startMinute = -1)), LocalDateTime.now()))
    }

    @Test
    fun foodStatusRequiresPreparationAndTerminalStatesStayClosed() {
        assertFalse(FoodOrderStatus.PLACED.canTransitionTo(FoodOrderStatus.READY))
        assertTrue(FoodOrderStatus.PREPARING.canTransitionTo(FoodOrderStatus.READY))
        assertFalse(FoodOrderStatus.COMPLETED.canTransitionTo(FoodOrderStatus.ACCEPTED))
        assertFalse(FoodOrderStatus.CANCELLED.active)
        assertEquals(3000L, cartTotal(listOf(CartItem(MenuItem(pricePaise = 1500), 2))))
    }

    @Test
    fun reportsRequireAssignmentAndResolutionSequence() {
        assertFalse(ReportStatus.SUBMITTED.canTransitionTo(ReportStatus.RESOLVED))
        assertTrue(ReportStatus.IN_PROGRESS.canTransitionTo(ReportStatus.RESOLVED))
        assertFalse(ReportStatus.RESOLVED.canTransitionTo(ReportStatus.SUBMITTED))
    }

    @Test
    fun etaRejectsStaleFutureAndMissingLocations() {
        val now = 1_000_000L
        val stop = BusStop(latitude = 12.0, longitude = 80.0)
        assertNull(etaMinutes(null, stop, now))
        assertNull(etaMinutes(BusLocation(12.0, 80.0, now - 121000), stop, now))
        assertNull(etaMinutes(BusLocation(12.0, 80.0, now + 1), stop, now))
        assertEquals(1, etaMinutes(BusLocation(12.0, 80.0, now), stop, now))
    }

    @Test
    fun activityUsesTypedDestination() {
        assertEquals("Order:order", destinationRoute(Destination.FOOD, "order"))
        assertEquals("Report:report", destinationRoute(Destination.REPORT, "report"))
        assertEquals("Event:event", destinationRoute(Destination.EVENT, "event"))
        assertEquals("Notice:notice", destinationRoute(Destination.ANNOUNCEMENT, "notice"))
        assertEquals("Bus", destinationRoute(Destination.TRANSPORT, "route"))
        assertNull(destinationRoute(null, "missing"))
    }
}
