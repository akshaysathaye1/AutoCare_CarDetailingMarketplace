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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegistrationSuccess: (String) -> Unit, // role: "CUSTOMER" or "GARAGE"
    onNavigateToLogin: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("CUSTOMER") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    fun submitRegistration() {
        if (isLoading) return

        // 1. Client-Side Field Validation
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()
        var hasError = false

        if (trimmedName.isBlank()) {
            nameError = "Full name is required."
            hasError = true
        } else {
            nameError = null
        }

        if (trimmedEmail.isBlank()) {
            emailError = "Email address is required."
            hasError = true
        } else if (!authRepository.isValidEmail(trimmedEmail)) {
            emailError = "Please enter a valid email address."
            hasError = true
        } else {
            emailError = null
        }

        if (trimmedPhone.isBlank()) {
            phoneError = "Phone number is required."
            hasError = true
        } else if (!authRepository.isValidPhone(trimmedPhone)) {
            phoneError = "Please enter a valid phone number (7-15 digits)."
            hasError = true
        } else {
            phoneError = null
        }

        if (password.isBlank()) {
            passwordError = "Password is required."
            hasError = true
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters long."
            hasError = true
        } else {
            passwordError = null
        }

        if (confirmPassword.isBlank()) {
            confirmPasswordError = "Please confirm your password."
            hasError = true
        } else if (confirmPassword != password) {
            confirmPasswordError = "Passwords do not match."
            hasError = true
        } else {
            confirmPasswordError = null
        }

        if (hasError) {
            errorMessage = "Please fix the input errors above."
            return
        }

        focusManager.clearFocus()
        isLoading = true
        errorMessage = null

        // 2. Submit Account Creation & Firestore Profile Storage with guaranteed state cleanup
        coroutineScope.launch {
            try {
                authRepository.registerUserSuspend(
                    name = trimmedName,
                    email = trimmedEmail,
                    password = password,
                    phone = trimmedPhone,
                    role = selectedRole
                ) { success, error ->
                    isLoading = false
                    if (success) {
                        AuthRepository.DiagnosticLogger.d("RegisterScreen", "6. Registration succeeded. Launching navigation for role: $selectedRole")
                        onRegistrationSuccess(selectedRole)
                    } else {
                        errorMessage = sanitizeRegisterError(error)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                isLoading = false
                errorMessage = sanitizeRegisterError(e.localizedMessage)
            } finally {
                // Guaranteed safety fallback to clear loading state
                if (errorMessage != null) {
                    isLoading = false
                }
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
            Spacer(modifier = Modifier.height(20.dp))

            // Branding Header with standard DirectionsCar icon
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "AutoCare Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Text(
                text = "Join AutoCare",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Create your account and select your role",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error Message Banner with Warning icon
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Error: $errorMessage" }
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

            // Input Form Card
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Account Details",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Full Name
                    CustomTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                            errorMessage = null
                        },
                        label = "Full Name",
                        placeholder = "e.g. John Doe",
                        isError = nameError != null,
                        errorMessage = nameError,
                        enabled = !isLoading,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Name",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Email Address
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Phone Number
                    CustomTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            phoneError = null
                            errorMessage = null
                        },
                        label = "Phone Number",
                        placeholder = "+91 9876543210",
                        isError = phoneError != null,
                        errorMessage = phoneError,
                        enabled = !isLoading,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Phone",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password
                    CustomTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                            errorMessage = null
                        },
                        label = "Password (min. 6 characters)",
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
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Confirm Password
                    CustomTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmPasswordError = null
                            errorMessage = null
                        },
                        label = "Confirm Password",
                        placeholder = "••••••••",
                        isError = confirmPasswordError != null,
                        errorMessage = confirmPasswordError,
                        enabled = !isLoading,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Confirm Password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { confirmPasswordVisible = !confirmPasswordVisible }
                                    .padding(8.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitRegistration() }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Selection UI
                    Text(
                        text = "I am registering as:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Customer Role Option
                        AppCard(
                            onClick = { selectedRole = "CUSTOMER" },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "Select Customer role" }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = if (selectedRole == "CUSTOMER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Customer",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (selectedRole == "CUSTOMER") FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedRole == "CUSTOMER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (selectedRole == "CUSTOMER") "● Selected" else "Book services",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedRole == "CUSTOMER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Garage Owner Role Option
                        AppCard(
                            onClick = { selectedRole = "GARAGE" },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "Select Garage Owner role" }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = if (selectedRole == "GARAGE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Garage Owner",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (selectedRole == "GARAGE") FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedRole == "GARAGE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (selectedRole == "GARAGE") "● Selected" else "Partner workshop",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedRole == "GARAGE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (selectedRole == "GARAGE") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ℹ Garage accounts access workshop management, service configuration, and bookings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Create Account Button
                    PrimaryButton(
                        text = "Create Account & Profile",
                        onClick = { submitRegistration() },
                        isLoading = isLoading,
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation back to Login
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(enabled = !isLoading) { onNavigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Sanitize registration error messages to present clear, user-friendly diagnostics
 * without misleading network claims or exposing internal stack traces.
 */
internal fun sanitizeRegisterError(rawError: String?): String {
    if (rawError.isNullOrBlank()) return "Registration failed. Please check your details and try again."
    val lower = rawError.lowercase()
    return when {
        lower.contains("email-already-in-use") || lower.contains("already exists") || lower.contains("already in use") ->
            "An account with this email address already exists. Please sign in instead."
        lower.contains("weak-password") || lower.contains("weak password") || lower.contains("password is too weak") ->
            "The password is too weak. Please use at least 6 characters."
        lower.contains("invalid-email") || lower.contains("invalid email") || lower.contains("enter a valid email") ->
            "Please provide a valid email address."
        lower.contains("cloud firestore") || lower.contains("firestore") ->
            rawError.substringBefore("\n").trim()
        lower.contains("permission denied") || lower.contains("database access") ->
            rawError.substringBefore("\n").trim()
        lower.contains("profile setup timed out") || lower.contains("profile save failed") ->
            rawError.substringBefore("\n").trim()
        lower.contains("firebasenetworkexception") || lower.contains("check your internet connection") ->
            "Network connection error. Please check your internet connection."
        lower.contains("timed out") || lower.contains("timeout") ->
            rawError.substringBefore("\n").trim()
        lower.contains("network") || lower.contains("unreachable") ->
            "Network connection error. Please check your internet connection."
        else ->
            rawError.substringBefore("\n").substringBefore("Exception").trim().ifBlank {
                "Unable to complete registration. Please try again."
            }
    }
}
