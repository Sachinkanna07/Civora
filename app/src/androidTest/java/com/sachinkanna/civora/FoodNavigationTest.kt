package com.sachinkanna.civora

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.food.FoodScreen
import com.sachinkanna.civora.viewmodel.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FoodNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val gateway = object : CampusGateway {
        override fun observe(feed: CampusFeed,profile: UserProfile): Flow<List<Any>> = flowOf(emptyList())
        override suspend fun action(name: String,data: Map<String,Any?>): Map<String,Any?> = error("No backend mutation is expected in navigation")
        override suspend fun updateProfile(uid: String,values: Map<String,Any?>) = Unit
        override suspend fun markRead(uid: String,notificationId: String) = Unit
        override suspend fun follow(uid: String,clubId: String,followed: Boolean) = Unit
    }
    @Test fun cartSurvivesStateRestorationAndBackReturnsThroughFoodStages() {
        val item=MenuItem(id="tea",canteenId="kitchen",name="Tea",pricePaise=1500,available=true)
        val state=WorkspaceState(feeds=mapOf(
            CampusFeed.CANTEENS to FeedState(false,listOf(Canteen(id="kitchen",name="Campus kitchen",open=true))),
            CampusFeed.MENU to FeedState(false,listOf(item)),
            CampusFeed.ORDERS to FeedState(false,emptyList())),cart=listOf(CartItem(item,1)))
        var exits=0
        val viewModel=CampusWorkspaceViewModel(gateway)
        val restore=StateRestorationTester(compose)
        restore.setContent { MaterialTheme {FoodScreen(state,viewModel,{exits++})} }
        compose.onNodeWithText("Continue your cart").performClick()
        compose.onNodeWithText("Your cart").assertIsDisplayed()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Your cart").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Campus food").assertIsDisplayed()
        compose.runOnIdle {assertEquals(0,exits)}
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle {assertEquals(1,exits)}
    }
}
