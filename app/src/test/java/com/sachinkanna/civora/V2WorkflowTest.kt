package com.sachinkanna.civora

import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.viewmodel.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V2WorkflowTest {
    @Test fun holidaysSkipRecurringClassesAndCurrentClassUsesExclusiveEnd() {
        val monday=LocalDateTime.of(2026,10,5,9,30)
        val c=TimetableEntry(id="math",dayOfWeek=1,startMinute=540,endMinute=600)
        assertEquals(c,currentClass(listOf(c),monday))
        assertNull(currentClass(listOf(c),monday.withHour(10).withMinute(0)))
        assertEquals(LocalDate.of(2026,10,12),nextClass(listOf(c),monday.minusHours(1),setOf(monday.toLocalDate()))!!.date.toLocalDate())
    }
    @Test fun semesterRangesAndDateOverridesProduceOneAuthoritativeDay() {
        val date=LocalDate.of(2026,10,12)
        val old=TimetableEntry(id="old",courseId="math",courseName="Math",dayOfWeek=1,startMinute=540,endMinute=600,effectiveFrom="2026-01-01",effectiveTo="2026-06-30")
        val active=old.copy(id="active",effectiveFrom="2026-07-01",effectiveTo="2026-12-31")
        val moved=AcademicOverride(id="move",date=date.toString(),kind="CLASS",baseCourseId="math",courseId="math",courseName="Math",faculty="Faculty",startMinute=660,endMinute=720,room="B201")
        assertEquals(listOf("override_move"),resolvedClassesForDate(listOf(old,active),listOf(moved),date).map {it.id})
        val cancelled=moved.copy(kind="CANCEL")
        assertTrue(resolvedClassesForDate(listOf(old,active),listOf(cancelled),date).isEmpty())
        assertEquals(11,nextAcademicClass(listOf(old,active),listOf(moved),date.atTime(10,30),emptySet())!!.date.hour)
    }
    @Test fun etaRequiresTheSelectedBoardingStopAndFreshLocation() {
        val route=BusRoute(stops=listOf(BusStop("First",13.0,80.0),BusStop("Library",13.01,80.01)))
        assertNull(selectedBoardingStop(route,""))
        assertNull(selectedBoardingStop(route,"Removed stop"))
        val selected=selectedBoardingStop(route,"Library")!!
        assertNull(etaMinutes(BusLocation(13.0,80.0,1),selected,180000))
        assertNotNull(etaMinutes(BusLocation(13.0,80.0,180000),selected,180001))
    }
    @Test fun cameraPayloadsArePurposeBoundAndAcceptCompositeRegistrationIds() {
        val event="civora:student_event:${"a".repeat(48)}"
        assertNotNull(parseTicketPayload(event))
        assertNull(parseTicketPayload(event,"civora-meal"))
        assertNull(parseTicketPayload("civora:../private:${"a".repeat(48)}"))
        assertNull(parseTicketPayload("civora:student_event:forged"))
        assertNotNull(parseTicketPayload("civora-meal:student:${"b".repeat(48)}","civora-meal"))
    }
    @Test fun rushQueueIsOldestFirstAndCompletedOrdersLeaveActiveQueues() {
        val orders=(1..20).map {FoodOrder(id="$it",createdAt=(21-it).toLong(),status=if(it==20)FoodOrderStatus.COMPLETED else FoodOrderStatus.PLACED)}
        val queue=kitchenQueue(orders,"New")
        assertEquals(19,queue.size);assertEquals("19",queue.first().id)
        assertEquals("20",kitchenQueue(orders,"Completed").single().id)
        assertEquals(FoodOrderStatus.PREPARING,nextKitchenStatus(FoodOrderStatus.ACCEPTED))
        assertNull(nextKitchenStatus(FoodOrderStatus.COMPLETED))
    }
    private class Gateway: CampusGateway {
        val menu=MutableStateFlow<List<Any>>(emptyList());var fail=true;val requests=mutableListOf<Map<String,Any?>>()
        override fun observe(feed: CampusFeed,profile: UserProfile): Flow<List<Any>> =if(feed==CampusFeed.MENU)menu else flowOf(emptyList())
        override suspend fun action(name: String,data: Map<String,Any?>): Map<String,Any?> {requests+=data;if(fail)throw IllegalStateException("offline");return mapOf("orderId" to "student_checkout")}
        override suspend fun updateProfile(uid: String,values: Map<String,Any?>) {}
        override suspend fun markRead(uid: String,notificationId: String) {}
        override suspend fun follow(uid: String,clubId: String,followed: Boolean) {}
    }
    @Test fun restoredCartRepricesAndRetriesUseOneCheckoutIdentity()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val original=MenuItem("meal","canteen","Meal",100,true)
            val persistence=MemoryCartPersistence();persistence.save("student",PersistedCart("checkout",listOf(CartItem(original,2))))
            val gateway=Gateway();gateway.menu.value=listOf(original.copy(pricePaise=150))
            val vm=CampusWorkspaceViewModel(gateway,persistence) {_,_->}
            vm.connect(UserProfile(uid="student"));runCurrent()
            assertEquals(300L,cartTotal(vm.state.value.cart));assertEquals("checkout",vm.state.value.cartRequestId)
            vm.placeOrder();runCurrent();assertTrue(vm.state.value.actionError)
            assertEquals("checkout",vm.state.value.cartRequestId);assertFalse(vm.state.value.cart.isEmpty())
            gateway.fail=false;vm.placeOrder();runCurrent()
            assertEquals(listOf("checkout","checkout"),gateway.requests.map {it["requestId"]})
            assertEquals(300L,gateway.requests.last()["expectedTotalPaise"])
            assertTrue(vm.state.value.cart.isEmpty());assertEquals("student_checkout",vm.state.value.lastOrderId)
            vm.disconnect();assertTrue(persistence.load("student").items.isEmpty())
        }finally {Dispatchers.resetMain()}
    }
    @Test fun deepLinksRejectPathsAndOpenAccountOwnedScreens() {
        assertEquals("Order:student_checkout",AppNavigation.route("food","student_checkout"))
        assertNull(AppNavigation.route("report","../other"))
        assertNull(AppNavigation.route("unknown","test"))
        assertEquals("Bus",AppNavigation.route("transport","bus"))
    }
}
