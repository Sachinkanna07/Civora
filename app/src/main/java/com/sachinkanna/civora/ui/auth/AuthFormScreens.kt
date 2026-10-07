package com.sachinkanna.civora.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.ui.components.CivoraGradientBackground

@Composable
fun LoginScreen(
    onBackClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onAuth: (String, String) -> Unit,
    error: String? = null,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
) =
    AuthFormScreen(
        title = "Welcome back",
        subtitle = "Sign in to continue to your Civora campus space.",
        submitLabel = "Sign In",
        alternatePrompt = "New to Civora?",
        alternateLabel = "Create account",
        onBackClick = onBackClick,
        onAlternateClick = onCreateAccountClick,
        onSubmit = { _, email, password -> onAuth(email, password) },
        error = error,
        loading = loading,
        showName = false,
        modifier = modifier,
    )

@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onSignInClick: () -> Unit,
    onAuth: (String, String, String) -> Unit,
    error: String? = null,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
) =
    AuthFormScreen(
        title = "Create your account",
        subtitle = "Set up your Civora profile to get started.",
        submitLabel = "Create Account",
        alternatePrompt = "Already have an account?",
        alternateLabel = "Sign in",
        onBackClick = onBackClick,
        onAlternateClick = onSignInClick,
        onSubmit = { name, email, password -> onAuth(name, email, password) },
        error = error,
        loading = loading,
        showName = true,
        modifier = modifier,
    )

@Composable
private fun AuthFormScreen(
    title: String,
    subtitle: String,
    submitLabel: String,
    alternatePrompt: String,
    alternateLabel: String,
    onBackClick: () -> Unit,
    onAlternateClick: () -> Unit,
    onSubmit: (String, String, String) -> Unit,
    showName: Boolean,
    error: String? = null,
    loading: Boolean = false,
    modifier: Modifier,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    val emailError =
        email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val passwordError = password.isNotBlank() && password.length < 8
    val confirmError = showName && confirmPassword.isNotBlank() && confirmPassword != password
    val canSubmit =
        (!showName || name.trim().length >= 2) &&
            email.isNotBlank() &&
            !emailError &&
            password.length >= 8 &&
            (!showName || (confirmPassword == password && confirmPassword.isNotBlank()))

    CivoraGradientBackground(modifier = modifier) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBackClick, contentPadding = PaddingValues(0.dp)) {
                Text("< Back")
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (showName) AuthField("Full name", name, { name = it }, KeyboardType.Text)
            AuthField(
                "Email address",
                email,
                { email = it },
                KeyboardType.Email,
                emailError,
                "Enter a valid email address",
            )
            PasswordField(
                "Password",
                password,
                { password = it },
                passwordVisible,
                { passwordVisible = !passwordVisible },
                passwordError,
                "Use at least 8 characters",
            )
            if (showName)
                PasswordField(
                    "Confirm password",
                    confirmPassword,
                    { confirmPassword = it },
                    confirmVisible,
                    { confirmVisible = !confirmVisible },
                    confirmError,
                    "Passwords do not match",
                )

            Button(
                onClick = { onSubmit(name, email, password) },
                enabled = canSubmit && !loading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text(if (loading) "Please wait..." else submitLabel)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(alternatePrompt, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onAlternateClick) { Text(alternateLabel) }
            }
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value,
        onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions =
            androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggle: () -> Unit,
    isError: Boolean,
    supportingText: String,
) {
    OutlinedTextField(
        value,
        onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = { if (isError) Text(supportingText) },
        visualTransformation =
            if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions =
            androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { TextButton(onClick = onToggle) { Text(if (visible) "Hide" else "Show") } },
        modifier = Modifier.fillMaxWidth(),
    )
}
