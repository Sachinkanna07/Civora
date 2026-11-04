package com.sachinkanna.civora

import com.sachinkanna.civora.data.model.CampusPolicy
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class CampusPolicyTest {
    @Test fun institutionalDomainIsExact() {
        assertTrue(CampusPolicy.institutionalEmail("Faculty@KPRIET.AC.IN"))
        listOf("a@evil.kpriet.ac.in","a@kpriet.ac.in.evil","a@other.ac.in","a@b@kpriet.ac.in").forEach {assertFalse(CampusPolicy.institutionalEmail(it))}
    }
    @Test fun approvedExceptionsRequireVerificationAndMatchingEmail() {
        assertTrue(CampusPolicy.eligible("operator@example.com",true,true,"operator@example.com"))
        assertFalse(CampusPolicy.eligible("operator@example.com",false,true,"operator@example.com"))
        assertFalse(CampusPolicy.eligible("changed@example.com",true,true,"operator@example.com"))
        assertFalse(CampusPolicy.eligible("a@kpriet.ac.in",false,false,""))
    }
    @Test fun campusTimeIgnoresDeviceTimezone() {
        val original=TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            val time=CampusPolicy.at(1704067200000L)
            assertEquals(2024,time.year);assertEquals(1,time.dayOfMonth)
            assertEquals(5,time.hour);assertEquals(30,time.minute)
        } finally {TimeZone.setDefault(original)}
    }
}
