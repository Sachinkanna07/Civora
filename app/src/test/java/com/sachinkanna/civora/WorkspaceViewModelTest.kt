package com.sachinkanna.civora

import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.viewmodel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModelTest {
    private class FakeGateway : CampusGateway {
        var fail = false
        var actions = 0
        var pending: CompletableDeferred<Unit>? = null

        override fun observe(feed: CampusFeed, profile: UserProfile): Flow<List<Any>> = flow {
            if (fail) throw IllegalStateException("secret backend failure")
            emit(if (feed == CampusFeed.CANTEENS) listOf(Canteen(id = "c")) else emptyList())
        }

        override suspend fun action(name: String, data: Map<String, Any?>): Map<String, Any?> {
            actions++
            pending?.await()
            if (fail) throw IllegalStateException("secret")
            return emptyMap()
        }

        override suspend fun updateProfile(uid: String, values: Map<String, Any?>) {}

        override suspend fun markRead(uid: String, notificationId: String) {}

        override suspend fun follow(uid: String, clubId: String, followed: Boolean) {}
    }

    @Test
    fun feedAndActionErrorsAreRecoverableAndDoNotExposeExceptions() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val fake = FakeGateway()
            val vm = CampusWorkspaceViewModel(fake) { _, _ -> }
            vm.connect(UserProfile(uid = "v", role = UserRole.VENDOR))
            assertTrue(vm.state.value.feed(CampusFeed.CANTEENS).loading)
            runCurrent()
            assertEquals("c", vm.state.value.values<Canteen>(CampusFeed.CANTEENS).single().id)
            fake.fail = true
            vm.retry(CampusFeed.CANTEENS)
            runCurrent()
            assertFalse(vm.state.value.feed(CampusFeed.CANTEENS).loading)
            assertNotNull(vm.state.value.feed(CampusFeed.CANTEENS).error)
            vm.action("updateFoodOrder", emptyMap())
            assertTrue(vm.state.value.busy)
            runCurrent()
            assertFalse(vm.state.value.busy)
            assertTrue(vm.state.value.actionError)
            assertFalse(vm.state.value.message!!.contains("secret"))
            fake.fail = false
            vm.retry(CampusFeed.CANTEENS)
            runCurrent()
            assertNull(vm.state.value.feed(CampusFeed.CANTEENS).error)
            vm.disconnect()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cancelledSessionCannotClearNewSessionsBusyState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val fake = FakeGateway()
            val vm = CampusWorkspaceViewModel(fake) { _, _ -> }
            vm.connect(UserProfile(uid = "first", role = UserRole.VENDOR))
            fake.pending = CompletableDeferred()
            vm.action("firstAction", emptyMap())
            runCurrent()
            vm.disconnect()
            vm.connect(UserProfile(uid = "second", role = UserRole.VENDOR))
            val secondAction = CompletableDeferred<Unit>()
            fake.pending = secondAction
            vm.action("secondAction", emptyMap())
            runCurrent()
            assertTrue(vm.state.value.busy)
            secondAction.complete(Unit)
            runCurrent()
            assertFalse(vm.state.value.busy)
            vm.disconnect()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cartIsSingleCanteenBoundedAndOrderClearsOnSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val fake = FakeGateway()
            val vm = CampusWorkspaceViewModel(fake) { _, _ -> }
            val item = MenuItem(id = "a", canteenId = "one", pricePaise = 100, available = true)
            vm.add(item, 30)
            assertEquals(20, vm.state.value.cart.single().quantity)
            vm.add(MenuItem(id = "b", canteenId = "two", available = true), 1)
            assertEquals(1, vm.state.value.cart.size)
            vm.placeOrder("request")
            runCurrent()
            assertTrue(vm.state.value.cart.isEmpty())
            assertEquals(1, fake.actions)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
