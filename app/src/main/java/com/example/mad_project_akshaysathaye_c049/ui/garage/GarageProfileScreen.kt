package com.example.mad_project_akshaysathaye_c049.ui.garage

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageImageProvider
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.data.model.Garage
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomTextField
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.RatingComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.SecondaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading
import kotlinx.coroutines.launch

/**
 * GarageProfileScreen — Displays and manages workshop partner details for authenticated garage owners.
 *
 * Capabilities:
 *  - Loads workshop information strictly scoped to the authenticated owner's UID from Firestore.
 *  - Supports workshop creation and real-time updates without mock data injection.
 *  - Validates contact and address details before persisting to /garages collection.
 *  - Provides account role-switching and sign-out controls.
 */
@Composable
fun GarageProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onSwitchRole: () -> Unit = {},
    onLogout: () -> Unit = {},
    authRepository: AuthRepository = remember { AuthRepository() },
    garageRepository: GarageRepository = remember { GarageRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var garage by remember { mutableStateOf<Garage?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEditDialogOpen by remember { mutableStateOf(false) }
    var isSavingWorkshop by remember { mutableStateOf(false) }

    fun loadGarage() {
        val ownerId = authRepository.getEffectiveUserId("GARAGE")
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                if (ownerId.isNotBlank()) {
                    val res = garageRepository.getGarageByOwnerId(ownerId)
                    garage = res.getOrNull()
                }

                // Match strictly by ownerId
                if (garage == null && ownerId.isNotBlank()) {
                    val all = garageRepository.getAllGarages().getOrDefault(emptyList())
                    garage = all.firstOrNull { it.ownerId == ownerId }
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to load workshop profile."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadGarage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AutoCareTopBar(
                title = "Workshop Profile",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(onClick = { loadGarage() }) {
                        Text(
                            text = "Refresh",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            )
        },
        bottomBar = {
            GarageBottomBar(
                currentRoute = "garage_profile",
                onNavigateToRoute = onNavigateToRoute
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading workshop profile...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            garage == null -> {
                // Empty state: Owner has not registered a garage yet
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Workshop Profile Configured",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Set up your workshop details to list detailing services and accept appointments on the AutoCare Marketplace.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            PrimaryButton(
                                text = "Setup Workshop Profile",
                                onClick = { isEditDialogOpen = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Account Actions
                    SectionHeading(title = "Account Settings")
                    Spacer(modifier = Modifier.height(10.dp))

                    AppCard {
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
                                Text("Switch to Customer View", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        SecondaryButton(
                            text = "Sign Out",
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            else -> {
                val currentGarage = garage!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Header Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Image(
                                painter = painterResource(id = GarageImageProvider.getGarageImageRes(currentGarage.id)),
                                contentDescription = currentGarage.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = currentGarage.name.ifBlank { "AutoCare Partner Workshop" },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Authorized Detailing & Service Partner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (currentGarage.reviewCount > 0) {
                                RatingComponent(
                                    rating = currentGarage.rating,
                                    reviewCount = currentGarage.reviewCount
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "New Workshop • No reviews yet",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { isEditDialogOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Workshop Details", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Contact & Location Card
                    SectionHeading(title = "Location & Contact Information")
                    Spacer(modifier = Modifier.height(10.dp))

                    AppCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Address",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    val addr = currentGarage.address.ifBlank { "Address not specified" }
                                    val city = currentGarage.city.ifBlank { "" }
                                    val fullAddr = if (city.isNotBlank()) "$addr, $city" else addr
                                    Text(
                                        text = fullAddr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Contact Phone",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = currentGarage.contactNumber.ifBlank { "Not provided" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Verified Partner ID",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = currentGarage.id.takeLast(10).uppercase(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Operating Hours
                    SectionHeading(title = "Operating Timings")
                    Spacer(modifier = Modifier.height(10.dp))

                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Daily Schedule", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = currentGarage.operatingHours.ifBlank { "09:00 AM – 07:00 PM" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Description & Facilities
                    SectionHeading(title = "About Workshop")
                    Spacer(modifier = Modifier.height(10.dp))

                    AppCard {
                        Text(
                            text = currentGarage.description.ifBlank {
                                "Specialized automotive detailing studio equipped with certified technicians and professional detailing products."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Account Actions
                    SectionHeading(title = "Account Settings")
                    Spacer(modifier = Modifier.height(10.dp))

                    AppCard {
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
                                Text("Switch to Customer View", fontWeight = FontWeight.SemiBold)
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

    // Edit / Create Workshop Profile Dialog
    if (isEditDialogOpen) {
        WorkshopProfileDialog(
            initialGarage = garage,
            isSaving = isSavingWorkshop,
            onDismiss = { isEditDialogOpen = false },
            onSave = { name, description, address, city, contact, hours ->
                val ownerId = authRepository.getEffectiveUserId("GARAGE")
                coroutineScope.launch {
                    isSavingWorkshop = true
                    try {
                        val base = garage ?: Garage(ownerId = ownerId)
                        val updated = base.copy(
                            ownerId = ownerId,
                            name = name,
                            description = description,
                            address = address,
                            city = city,
                            contactNumber = contact,
                            operatingHours = hours
                        )
                        val result = garageRepository.createGarage(updated)
                        if (result.isSuccess) {
                            isEditDialogOpen = false
                            snackbarHostState.showSnackbar("Workshop profile saved successfully!")
                            loadGarage()
                        } else {
                            snackbarHostState.showSnackbar(
                                "Failed to save workshop: ${result.exceptionOrNull()?.localizedMessage}"
                            )
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
                    } finally {
                        isSavingWorkshop = false
                    }
                }
            }
        )
    }
}

@Composable
fun WorkshopProfileDialog(
    initialGarage: Garage?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, address: String, city: String, contact: String, hours: String) -> Unit
) {
    var name by remember { mutableStateOf(initialGarage?.name.orEmpty()) }
    var description by remember { mutableStateOf(initialGarage?.description.orEmpty()) }
    var address by remember { mutableStateOf(initialGarage?.address.orEmpty()) }
    var city by remember { mutableStateOf(initialGarage?.city.orEmpty()) }
    var contact by remember { mutableStateOf(initialGarage?.contactNumber.orEmpty()) }
    var hours by remember { mutableStateOf(initialGarage?.operatingHours?.ifBlank { "09:00 AM - 07:00 PM" } ?: "09:00 AM - 07:00 PM") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var cityError by remember { mutableStateOf<String?>(null) }
    var contactError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = if (initialGarage == null) "Setup Workshop Profile" else "Edit Workshop Profile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CustomTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = "Workshop Name",
                    placeholder = "e.g. Apex Auto Detailing Studio",
                    isError = nameError != null,
                    errorMessage = nameError
                )

                CustomTextField(
                    value = contact,
                    onValueChange = {
                        contact = it
                        contactError = null
                    },
                    label = "Contact Phone Number",
                    placeholder = "e.g. 9876543210",
                    isError = contactError != null,
                    errorMessage = contactError
                )

                CustomTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        addressError = null
                    },
                    label = "Workshop Address",
                    placeholder = "e.g. 102 Hosur Road, Electronic City",
                    isError = addressError != null,
                    errorMessage = addressError
                )

                CustomTextField(
                    value = city,
                    onValueChange = {
                        city = it
                        cityError = null
                    },
                    label = "City",
                    placeholder = "e.g. Bangalore",
                    isError = cityError != null,
                    errorMessage = cityError
                )

                CustomTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = "Operating Hours",
                    placeholder = "09:00 AM - 07:00 PM"
                )

                CustomTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "About / Studio Description",
                    placeholder = "Describe your detailing bays, equipment, and specialties"
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (isSaving) "Saving..." else "Save Workshop",
                onClick = {
                    var hasError = false
                    if (name.trim().isBlank()) {
                        nameError = "Workshop name is required."
                        hasError = true
                    }
                    if (contact.trim().isBlank()) {
                        contactError = "Contact phone number is required."
                        hasError = true
                    } else if (!AuthRepository.isValidPhone(contact.trim())) {
                        contactError = "Please enter a valid phone number (7-15 digits)."
                        hasError = true
                    }
                    if (address.trim().isBlank()) {
                        addressError = "Address is required."
                        hasError = true
                    }
                    if (city.trim().isBlank()) {
                        cityError = "City is required."
                        hasError = true
                    }

                    if (!hasError && !isSaving) {
                        onSave(
                            name.trim(),
                            description.trim(),
                            address.trim(),
                            city.trim(),
                            contact.trim(),
                            hours.trim()
                        )
                    }
                },
                modifier = Modifier.width(150.dp)
            )
        },
        dismissButton = {
            if (!isSaving) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
