package com.sachinkanna.civora

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import com.sachinkanna.civora.ui.auth.RegisterScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RegistrationScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun publicRegistrationSubmitsIdentityWithoutPrivilegedRoleSelection() {
        var submitted: List<String>? = null
        compose.setContent {
            MaterialTheme {
                RegisterScreen(
                    onBackClick = {},
                    onSignInClick = {},
                    onAuth = { name, email, password -> submitted = listOf(name, email, password) },
                )
            }
        }
        listOf("Admin", "Faculty", "Vendor", "Driver").forEach {
            compose.onNodeWithText(it).assertDoesNotExist()
        }
        compose.onNodeWithText("Full name").performTextInput("Campus Student")
        compose.onNodeWithText("Email address").performTextInput("student@campus.edu")
        compose.onNodeWithText("Password", useUnmergedTree = false).performTextInput("CampusPass123")
        compose.onNodeWithText("Confirm password").performTextInput("CampusPass123")
        compose.onNodeWithText("Create Account").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(listOf("Campus Student", "student@campus.edu", "CampusPass123"), submitted)
        }
    }
}
