package com.sachinkanna.civora

import com.sachinkanna.civora.data.model.Announcement
import com.sachinkanna.civora.data.model.CampusEvent
import com.sachinkanna.civora.viewmodel.AnnouncementUiState
import com.sachinkanna.civora.viewmodel.EventsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampusModuleStateTest {
    @Test fun announcementFiltersByCategoryAndDepartment() {
        val state = AnnouncementUiState(items = listOf(Announcement(title="A", category="Academic", department="CSE"), Announcement(title="B", category="General")))
        assertEquals("A", state.copy(category="Academic").filtered.single().title)
        assertTrue(state.copy(departmentOnly=true).filtered.single().department == "CSE")
    }

    @Test fun eventFilteringKeepsSelectedCategory() {
        val state = EventsUiState(items = listOf(CampusEvent(title="Workshop", category="Workshop"), CampusEvent(title="Fest", category="Cultural")), upcomingOnly=false)
        assertEquals("Workshop", state.copy(selectedCategory="Workshop").filtered.single().title)
    }

    @Test fun registeredIdsRepresentDuplicatePreventionState() {
        val state = EventsUiState(registeredIds=setOf("event-1"))
        assertTrue("event-1" in state.registeredIds)
    }
}
