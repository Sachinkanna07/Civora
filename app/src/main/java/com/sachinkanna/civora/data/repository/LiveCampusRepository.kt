package com.sachinkanna.civora.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.google.firebase.functions.FirebaseFunctions
import com.sachinkanna.civora.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

enum class CampusFeed {
    TIMETABLE,
    ANNOUNCEMENTS,
    EVENTS,
    REGISTRATIONS,
    CANTEENS,
    MENU,
    ORDERS,
    REPORTS,
    ACTIVITIES,
    INBOX,
    CLUBS,
    FOLLOWS,
    ROUTES,
    BUSES,
    TRIPS,
    LOCATIONS,
    STUDENTS, STAFF, DEPARTMENTS, CALENDAR, ACADEMIC_OVERRIDES, TRANSPORT_UPDATES, MESS_HALLS, MEAL_PERIODS, ENTITLEMENTS, MEAL_CHECKINS,
}

data class Ticket(val id: String, val eventId: String, val token: String, val attended: Boolean, val userName: String="", val eventTitle: String="")

interface CampusGateway {
    fun observe(feed: CampusFeed, profile: UserProfile): Flow<List<Any>>

    suspend fun action(name: String, data: Map<String, Any?>): Map<String, Any?>

    suspend fun updateProfile(uid: String, values: Map<String, Any?>)

    suspend fun markRead(uid: String, notificationId: String)

    suspend fun follow(uid: String, clubId: String, followed: Boolean)
}

class LiveCampusRepository : CampusGateway {
    private val db
        get() = FirebaseFirestore.getInstance()

    override fun observe(feed: CampusFeed, profile: UserProfile): Flow<List<Any>> = callbackFlow {
        val registration =
            query(feed, profile).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                try {
                    trySend(snapshot?.documents.orEmpty().map { decode(feed, it) })
                } catch (e: Exception) {
                    close(e)
                }
            }
        awaitClose { registration.remove() }
    }

    private fun query(feed: CampusFeed, p: UserProfile): Query {
        val staff = p.role == UserRole.ADMIN
        return when (feed) {
            CampusFeed.TIMETABLE ->
                db.collection("timetables").let { if (staff) it else it
                    .whereEqualTo("department", p.department)
                    .whereEqualTo("year", p.year)
                    .whereEqualTo("section", p.section)
                }
            CampusFeed.ANNOUNCEMENTS ->
                db.collection("announcements")
                    .whereEqualTo("audience", "students")
                    .let { if (staff) it else it.whereIn("department", listOf("", p.department).distinct()) }
                    .orderBy("createdAt", Query.Direction.DESCENDING)
            CampusFeed.EVENTS ->
                db.collection("events")
                    .whereGreaterThanOrEqualTo("endTime", Timestamp.now())
                    .orderBy("endTime")
            CampusFeed.REGISTRATIONS ->
                db.collection("eventRegistrations").let {
                    if (staff) it
                    else
                        it.whereEqualTo(
                            if (p.role == UserRole.FACULTY) "organizerId" else "userId",
                            p.uid,
                        )
                }
            CampusFeed.CANTEENS ->
                db.collection("canteens").let {
                    if (p.role == UserRole.VENDOR) it.whereEqualTo("vendorId", p.uid) else it
                }
            CampusFeed.MENU -> db.collection("menuItems")
            CampusFeed.ORDERS ->
                db.collection("foodOrders")
                    .let {
                        if (staff) it
                        else
                            it.whereEqualTo(
                                if (p.role == UserRole.VENDOR) "vendorId" else "userId",
                                p.uid,
                            )
                    }
                    .orderBy("createdAt", Query.Direction.DESCENDING)
            CampusFeed.REPORTS ->
                db.collection("campusReports")
                    .let { if (staff) it else it.whereEqualTo(if(p.role == UserRole.MAINTENANCE) "assignedTo" else "createdBy", p.uid) }
                    .orderBy("createdAt", Query.Direction.DESCENDING)
            CampusFeed.ACTIVITIES ->
                db.collection("activities")
                    .whereEqualTo("userId", p.uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
            CampusFeed.INBOX ->
                db.collection("notifications")
                    .whereEqualTo("userId", p.uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
            CampusFeed.CLUBS -> db.collection("clubs").orderBy("name")
            CampusFeed.FOLLOWS -> db.collection("clubFollowers").whereEqualTo("userId", p.uid)
            CampusFeed.ROUTES -> db.collection("busRoutes")
            CampusFeed.BUSES ->
                db.collection("buses").let {
                    if (p.role == UserRole.DRIVER) it.whereEqualTo("driverId", p.uid) else it
                }
            CampusFeed.TRIPS -> db.collection("activeTrips").whereEqualTo("active", true)
            CampusFeed.LOCATIONS -> db.collection("campusLocations").orderBy("name")
            CampusFeed.STUDENTS -> db.collection("users").whereEqualTo("role","student").orderBy("name")
            CampusFeed.STAFF -> db.collection("staffDirectory").orderBy("name")
            CampusFeed.DEPARTMENTS -> db.collection("departments").orderBy("name")
            CampusFeed.CALENDAR -> db.collection("academicCalendar").orderBy("date")
            CampusFeed.ACADEMIC_OVERRIDES -> db.collection("academicOverrides").let {if(staff)it else it.whereEqualTo("department",p.department).whereEqualTo("year",p.year).whereIn("section",listOf("",p.section).distinct())}.orderBy("date")
            CampusFeed.TRANSPORT_UPDATES -> db.collection("transportUpdates").orderBy("createdAt", Query.Direction.DESCENDING)
            CampusFeed.MESS_HALLS -> db.collection("messHalls").let { if(p.role == UserRole.MESS) it.whereEqualTo("operatorId", p.uid) else it }
            CampusFeed.MEAL_PERIODS -> db.collection("mealPeriods")
            CampusFeed.ENTITLEMENTS -> db.collection("mealEntitlements").let { if(staff) it else it.whereEqualTo("userId", p.uid) }
            CampusFeed.MEAL_CHECKINS -> db.collection("mealCheckIns").whereEqualTo(if(p.role == UserRole.MESS) "operatorId" else "userId", p.uid).orderBy("createdAt", Query.Direction.DESCENDING)
        }.limit(100)
    }

    @Suppress("UNCHECKED_CAST")
    private fun decode(feed: CampusFeed, d: DocumentSnapshot): Any =
        when (feed) {
            CampusFeed.ANNOUNCEMENTS -> d.announcement()
            CampusFeed.EVENTS -> d.event()
            CampusFeed.TIMETABLE -> d.toObject(TimetableEntry::class.java)!!.copy(id = d.id)
            CampusFeed.REGISTRATIONS ->
                Ticket(
                    d.id,
                    d.getString("eventId").orEmpty(),
                    d.getString("token").orEmpty(),
                    d.getBoolean("attended") ?: false,
                    d.getString("userName").orEmpty(),
                    d.getString("eventTitle").orEmpty(),
                )
            CampusFeed.CANTEENS -> d.toObject(Canteen::class.java)!!.copy(id = d.id)
            CampusFeed.MENU -> d.toObject(MenuItem::class.java)!!.copy(id = d.id)
            CampusFeed.ORDERS ->
                FoodOrder(
                    d.id,
                    d.getString("userId").orEmpty(),
                    d.getString("canteenId").orEmpty(),
                    d.getString("token").orEmpty(),
                    FoodOrderStatus.entries.find { it.name == d.getString("status") } ?: FoodOrderStatus.PLACED,
                    (d.get("items") as? List<Map<String, Any>>).orEmpty().map {
                        FoodOrderItem(
                            it["menuItemId"] as? String ?: "",
                            it["name"] as? String ?: "",
                            (it["quantity"] as? Number)?.toInt() ?: 0,
                            (it["pricePaise"] as? Number)?.toLong() ?: 0,
                        )
                    },
                    d.getLong("totalPaise") ?: 0,
                    d.time("createdAt"),
                    d.getString("paymentState") ?: "PAY_AT_COUNTER",
                    d.getString("canteenName").orEmpty(),
                    d.getLong("preparationMinutes")?.toInt(),
                    d.time("updatedAt"),
                )
            CampusFeed.REPORTS ->
                CampusReport(
                    d.id,
                    d.getString("title").orEmpty(),
                    d.getString("description").orEmpty(),
                    ReportCategory.entries.find { it.name == d.getString("category") } ?: ReportCategory.OTHER,
                    d.getString("imageUrl").orEmpty(),
                    d.getString("location").orEmpty(),
                    d.getString("createdBy").orEmpty(),
                    d.time("createdAt"),
                    ReportStatus.entries.find { it.name == d.getString("status") } ?: ReportStatus.SUBMITTED,
                    d.getString("assignedTo").orEmpty(),
                    d.getString("resolutionNote").orEmpty(),
                    d.getString("imagePath").orEmpty(),
                    d.getString("completionImagePath").orEmpty(),
                    d.getString("assignedToName").orEmpty(),
                    d.getString("progressNote").orEmpty(),
                    d.getString("priority") ?: "normal",
                    d.time("updatedAt"),
                )
            CampusFeed.ACTIVITIES ->
                CampusActivity(
                    d.id,
                    d.getString("title").orEmpty(),
                    d.getString("body").orEmpty(),
                    ActivityType.entries.find { it.name == d.getString("type") } ?: ActivityType.NOTICE,
                    d.getString("status").orEmpty(),
                    d.time("createdAt"),
                    destination(d),
                    d.getString("entityId").orEmpty(),
                )
            CampusFeed.INBOX ->
                InboxItem(
                    d.id,
                    d.getString("title").orEmpty(),
                    d.getString("body").orEmpty(),
                    NotificationCategory.entries.find { it.name == d.getString("category") } ?: NotificationCategory.RELEVANT,
                    d.time("createdAt"),
                    d.getBoolean("read") ?: false,
                    destination(d),
                    d.getString("entityId").orEmpty(),
                )
            CampusFeed.CLUBS -> d.toObject(Club::class.java)!!.copy(id = d.id)
            CampusFeed.FOLLOWS -> d.getString("clubId").orEmpty()
            CampusFeed.ROUTES -> d.toObject(BusRoute::class.java)!!.copy(id = d.id)
            CampusFeed.BUSES -> d.toObject(Bus::class.java)!!.copy(id = d.id)
            CampusFeed.TRIPS ->
                ActiveTrip(
                    d.id,
                    d.getString("busId").orEmpty(),
                    d.getString("routeId").orEmpty(),
                    d.getString("driverId").orEmpty(),
                    d.getBoolean("active") ?: false,
                    (d.get("latitude") as? Number)?.let {
                        BusLocation(
                            it.toDouble(),
                            (d.get("longitude") as? Number)?.toDouble() ?: 0.0,
                            d.time("locationUpdatedAt"),
                            d.getString("locationSource") == "SIMULATED",
                        )
                    },
                )
            CampusFeed.LOCATIONS -> d.toObject(CampusLocation::class.java)!!.copy(id = d.id)
            CampusFeed.STUDENTS -> StaffMember(d.id,d.getString("name").orEmpty(),"student",d.getString("email").orEmpty())
            CampusFeed.STAFF -> d.toObject(StaffMember::class.java)!!.copy(uid = d.id)
            CampusFeed.DEPARTMENTS -> d.toObject(Department::class.java)!!.copy(id = d.id)
            CampusFeed.CALENDAR -> d.toObject(CalendarDay::class.java)!!.copy(id = d.id)
            CampusFeed.ACADEMIC_OVERRIDES -> d.toObject(AcademicOverride::class.java)!!.copy(id=d.id)
            CampusFeed.TRANSPORT_UPDATES -> TransportUpdate(d.id,d.getString("busId").orEmpty(),d.getString("routeId").orEmpty(),d.getString("busName").orEmpty(),d.getString("kind").orEmpty(),(d.getLong("delayMinutes")?:0).toInt(),d.getString("note").orEmpty(),d.time("createdAt"))
            CampusFeed.MESS_HALLS -> d.toObject(MessHall::class.java)!!.copy(id = d.id)
            CampusFeed.MEAL_PERIODS -> MealPeriod(d.id,d.getString("messId").orEmpty(),d.getString("label").orEmpty(),d.time("opensAt"),d.time("closesAt"))
            CampusFeed.ENTITLEMENTS -> d.toObject(MealEntitlement::class.java)!!.copy(id = d.id)
            CampusFeed.MEAL_CHECKINS -> MealCheckIn(d.id,d.getString("periodId").orEmpty(),d.getString("messId").orEmpty(),d.getString("userId").orEmpty(),d.getString("label").orEmpty(),d.time("createdAt"))
        }

    private fun destination(d: DocumentSnapshot) =
        d.getString("destination")?.let { value -> Destination.entries.find { it.name == value } }

    @Suppress("UNCHECKED_CAST")
    override suspend fun action(name: String, data: Map<String, Any?>): Map<String, Any?> =
        FirebaseFunctions.getInstance().getHttpsCallable(name).call(data).await().getData()
            as? Map<String, Any?> ?: emptyMap()

    override suspend fun updateProfile(uid: String, values: Map<String, Any?>) {
        db.collection("users").document(uid).update(values).await()
    }

    override suspend fun markRead(uid: String, notificationId: String) {
        db.collection("notifications").document(notificationId).update("read", true).await()
    }

    override suspend fun follow(uid: String, clubId: String, followed: Boolean) {
        val ref = db.collection("clubFollowers").document("${clubId}_$uid")
        if (followed) ref.delete().await()
        else
            ref.set(
                    mapOf(
                        "clubId" to clubId,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
    }
}
