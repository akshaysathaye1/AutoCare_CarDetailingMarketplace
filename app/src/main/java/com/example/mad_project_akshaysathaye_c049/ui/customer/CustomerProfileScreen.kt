package com.example.mad_project_akshaysathaye_c049.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.data.model.User
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomerBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomTextField
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.SecondaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CustomerProfileScreen — Comprehensive customer profile viewing and management screen.
 *
 * Security & Functional Requirements:
 *  - Enforces that users can ONLY view and update their own profile (scoped by active Firebase Auth UID).
 *  - Supports viewing profile metadata (email, role, registration timestamp) and editing full name & phone.
 *  - Validates full name and phone number inputs prior to invoking Firestore.
 *  - Displays clear loading, success, and error states.
 *  - Provides role-switching and sign-out controls while preserving core navigation.
 */
@Composable
fun CustomerProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onSwitchRole: () -> Unit,
    onLogout: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var userProfile by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    fun loadProfile() {
        val uid = authRepository.getCurrentUserId()
        if (uid.isNullOrBlank()) {
            isLoading = false
            errorMessage = "No active customer session. Please log in to view your profile."
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = authRepository.getUserProfileAsync(uid)
            if (result.isSuccess) {
                val profile = result.getOrNull()
                userProfile = profile
                editName = profile?.name ?: ""
                editPhone = profile?.phoneNumber ?: ""
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load profile."
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadProfile()
    }

    fun validateInputs(): Boolean {
        var valid = true

        val trimmedName = editName.trim()
        if (trimmedName.isBlank()) {
            nameError = "Full name cannot be blank."
            valid = false
        } else if (trimmedName.length < 2) {
            nameError = "Full name must be at least 2 characters."
            valid = false
        } else {
            nameError = null
        }

        val trimmedPhone = editPhone.trim()
        if (trimmedPhone.isNotBlank() && !AuthRepository.isValidPhone(trimmedPhone)) {
            phoneError = "Please enter a valid phone number (7-15 digits)."
            valid = false
        } else {
            phoneError = null
        }

        return valid
    }

    fun saveProfile() {
        if (!validateInputs()) return

        coroutineScope.launch {
            isSaving = true
            errorMessage = null
            successMessage = null

            // Updates the profile for the authenticated user only
            val result = authRepository.updateUserProfileAsync(editName, editPhone)
            isSaving = false

            if (result.isSuccess) {
                userProfile = userProfile?.copy(
                    name = editName.trim(),
                    phoneNumber = editPhone.trim()
                )
                isEditing = false
                successMessage = "Profile updated successfully!"
                snackbarHostState.showSnackbar("Profile changes saved.")
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to save profile changes."
            }
        }
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "Customer Profile",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    if (!isEditing && userProfile != null) {
                        TextButton(onClick = { isEditing = true }) {
                            Text(
                                text = "Edit",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            CustomerBottomBar(
                currentRoute = "customer_profile",
                onNavigateToRoute = onNavigateToRoute
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading customer profile...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Success Alert Banner
                if (successMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = successMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Error Alert Banner
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Header Profile Card
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val initials = userProfile?.name?.take(2)?.uppercase() ?: "C"
                                Text(
                                    text = initials,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = userProfile?.name?.ifBlank { "AutoCare Customer" } ?: "AutoCare Customer",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = userProfile?.email?.ifBlank { "Registered Customer" } ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = " Verified ${userProfile?.role ?: "CUSTOMER"} ",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Profile Information / Edit Form
                SectionHeading(title = if (isEditing) "Edit Account Details" else "Account Information")
                Spacer(modifier = Modifier.height(10.dp))

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    if (isEditing) {
                        // Editable Full Name
                        CustomTextField(
                            value = editName,
                            onValueChange = {
                                editName = it
                                if (nameError != null) nameError = null
                            },
                            label = "Full Name",
                            placeholder = "Enter your full name",
                            isError = nameError != null,
                            errorMessage = nameError
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Editable Phone Number
                        CustomTextField(
                            value = editPhone,
                            onValueChange = {
                                editPhone = it
                                if (phoneError != null) phoneError = null
                            },
                            label = "Phone Number",
                            placeholder = "e.g. 9876543210",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = phoneError != null,
                            errorMessage = phoneError
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Read-only email notice
                        Text(
                            text = "Email address (${userProfile?.email}) is linked to your Firebase Authentication credentials and cannot be changed here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save and Cancel buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SecondaryButton(
                                text = "Cancel",
                                onClick = {
                                    isEditing = false
                                    editName = userProfile?.name ?: ""
                                    editPhone = userProfile?.phoneNumber ?: ""
                                    nameError = null
                                    phoneError = null
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isSaving
                            )

                            PrimaryButton(
                                text = if (isSaving) "Saving..." else "Save Changes",
                                onClick = { saveProfile() },
                                modifier = Modifier.weight(1f),
                                enabled = !isSaving
                            )
                        }
                    } else {
                        // View Mode Details
                        ProfileDetailRow(label = "Full Name", value = userProfile?.name?.ifBlank { "Not provided" } ?: "Not provided")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        ProfileDetailRow(label = "Email Address", value = userProfile?.email?.ifBlank { "Not provided" } ?: "Not provided")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        ProfileDetailRow(label = "Phone Number", value = userProfile?.phoneNumber?.ifBlank { "Not provided" } ?: "Not provided")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        val memberDate = if ((userProfile?.createdAt ?: 0L) > 0L) {
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(userProfile!!.createdAt))
                        } else "Recent"
                        ProfileDetailRow(label = "Member Since", value = memberDate)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quick Navigation Shortcuts
                SectionHeading(title = "Quick Actions")
                Spacer(modifier = Modifier.height(10.dp))

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onNavigateToVehicles,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manage My Vehicles", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onNavigateToBookings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Booking History", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Account Management & Sign Out
                SectionHeading(title = "Account Settings")
                Spacer(modifier = Modifier.height(10.dp))

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onSwitchRole,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Switch Role / Workshop View", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SecondaryButton(
                        text = "Sign Out",
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
