package com.example.mad_project_akshaysathaye_c049.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomTextField
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit, // role: "CUSTOMER" or "GARAGE"
    onNavigateToRegister: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    fun submitLogin() {
        if (isLoading) return

        // 1. Client-Side Field Validation
        val trimmedEmail = email.trim()
        var hasError = false

        if (trimmedEmail.isBlank()) {
            emailError = "Email address is required."
            hasError = true
        } else if (!authRepository.isValidEmail(trimmedEmail)) {
            emailError = "Please enter a valid email address."
            hasError = true
        } else {
            emailError = null
        }

        if (password.isBlank()) {
            passwordError = "Password is required."
            hasError = true
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters."
            hasError = true
        } else {
            passwordError = null
        }

        if (hasError) {
            errorMessage = "Please resolve the input issues above."
            return
        }

        focusManager.clearFocus()
        isLoading = true
        errorMessage = null

        // 2. Authenticate with Firebase & Validate Stored Role
        authRepository.loginUser(trimmedEmail, password) { success, role, error ->
            isLoading = false
            if (success && role != null) {
                onLoginSuccess(role)
            } else {
                errorMessage = sanitizeAuthError(error)
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // AutoCare Brand Header with standard Material DirectionsCar symbol
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "AutoCare Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Text(
                text = "AutoCare",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Car Detailing & Workshop Marketplace",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error Message Banner (Clean Material Warning symbol)
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Error message: $errorMessage" }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Credentials Card
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Sign In to Your Account",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Input with standard Material Email symbol
                    CustomTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                            errorMessage = null
                        },
                        label = "Email Address",
                        placeholder = "name@example.com",
                        isError = emailError != null,
                        errorMessage = emailError,
                        enabled = !isLoading,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Input with Visibility Control and standard Material Lock/Visibility symbols
                    CustomTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                            errorMessage = null
                        },
                        label = "Password",
                        placeholder = "••••••••",
                        isError = passwordError != null,
                        errorMessage = passwordError,
                        enabled = !isLoading,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { passwordVisible = !passwordVisible }
                                    .padding(8.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitLogin() }
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Action Button
                    PrimaryButton(
                        text = "Sign In",
                        onClick = { submitLogin() },
                        isLoading = isLoading,
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Demo Access Mode Card (Provides immediate evaluation access without requiring live email creation)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quick Demo Access (Offline & Evaluation)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isLoading = true
                                authRepository.loginUser("customer_test@autocare.com", "Password@123") { success, role, _ ->
                                    isLoading = false
                                    onLoginSuccess(role ?: "CUSTOMER")
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text("Customer")
                        }
                        OutlinedButton(
                            onClick = { onLoginSuccess("GARAGE") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text("Partner")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation to Registration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Register",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(enabled = !isLoading) { onNavigateToRegister() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Sanitize raw Firebase or system errors into safe, user-friendly feedback without technical stack traces.
 */
internal fun sanitizeAuthError(rawError: String?): String {
    if (rawError.isNullOrBlank()) return "Authentication failed. Please check your credentials."
    val lower = rawError.lowercase()
    return when {
        lower.contains("credential") || lower.contains("supplied auth credential") || lower.contains("password") || lower.contains("invalid-credential") || lower.contains("wrong") || lower.contains("incorrect") || lower.contains("expired") || lower.contains("malformed") ->
            "Invalid email or password. Please verify your credentials and try again."
        lower.contains("no account found") || lower.contains("user-not-found") || lower.contains("no user record") ->
            "No account found with this email address. Please check your email or register."
        lower.contains("cloud firestore") || lower.contains("firestore") ->
            rawError.substringBefore("\n").trim()
        lower.contains("too-many-requests") || lower.contains("too many failed") ->
            "Too many failed login attempts. Please wait a few minutes before trying again."
        lower.contains("user-disabled") ->
            "This account has been disabled. Please contact AutoCare support."
        lower.contains("operation-not-allowed") || lower.contains("not enabled") ->
            "Email/Password authentication is disabled in Firebase Console."
        lower.contains("invalid or missing role") ->
            "Your account profile has an invalid role. Please contact support."
        lower.contains("profile is missing") ->
            "Account profile was not found. Please contact support or register again."
        lower.contains("firebasenetworkexception") || lower.contains("check your internet connection") ->
            "Network connection error. Please check your internet connection."
        lower.contains("network") || lower.contains("unreachable") ->
            "Network connection error. Please check your internet connection."
        lower.contains("timed out") || lower.contains("timeout") ->
            rawError.substringBefore("\n").trim()
        else ->
            rawError.substringBefore("\n").substringBefore("Exception").trim().ifBlank {
                "Unable to sign in. Please verify your details."
            }
    }
}
