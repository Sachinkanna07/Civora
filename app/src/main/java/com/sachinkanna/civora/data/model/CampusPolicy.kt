package com.sachinkanna.civora.data.model

import com.sachinkanna.civora.BuildConfig
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

object CampusPolicy {
    val zone: ZoneId = ZoneId.of(BuildConfig.CAMPUS_TIMEZONE)
    fun now(): LocalDateTime = LocalDateTime.now(zone)
    fun at(time: Long): LocalDateTime = Instant.ofEpochMilli(time).atZone(zone).toLocalDateTime()
    fun canonicalEmail(email: String): String = email.trim().lowercase(Locale.ROOT)
    fun institutionalEmail(email: String): Boolean {
        val parts=canonicalEmail(email).split('@')
        return parts.size==2&&parts[0].isNotBlank()&&parts[1]==BuildConfig.CAMPUS_EMAIL_DOMAIN
    }
    fun eligible(email: String,verified: Boolean,exception: Boolean,approvedEmail: String): Boolean =
        verified&&(institutionalEmail(email)||(exception&&canonicalEmail(email)==canonicalEmail(approvedEmail)))
}
