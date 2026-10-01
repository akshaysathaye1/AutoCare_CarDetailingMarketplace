package com.example.mad_project_akshaysathaye_c049.ui.garage

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.mad_project_akshaysathaye_c049.data.model.Garage
import com.example.mad_project_akshaysathaye_c049.data.model.Service
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.ServiceRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomTextField
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.PriceComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading
import kotlinx.coroutines.launch

@Composable
fun ManageServicesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    garageRepository: GarageRepository = remember { GarageRepository() },
    serviceRepository: ServiceRepository = remember { ServiceRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentGarage by remember { mutableStateOf<Garage?>(null) }
    var servicesList by remember { mutableStateOf<List<Service>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog state: null when closed, non-null (either existing service to edit or blank service for add)
    var isDialogVisible by remember { mutableStateOf(false) }
    var serviceBeingEdited by remember { mutableStateOf<Service?>(null) }

    // Delete confirmation state
    var serviceToDelete by remember { mutableStateOf<Service?>(null) }

    // Resolve current garage and load services
    fun loadGarageAndServices() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val currentUserId = authRepository.getCurrentUserId() ?: ""
                var garage: Garage? = null

                if (currentUserId.isNotBlank()) {
                    val garageRes = garageRepository.getGarageByOwnerId(currentUserId)
                    garage = garageRes.getOrNull()
                }

                // Fallback for mock/test data if owner has not registered a garage record yet
                if (garage == null) {
                    val allGaragesRes = garageRepository.getAllGarages()
                    val allGarages = allGaragesRes.getOrDefault(emptyList())
                    garage = allGarages.firstOrNull { it.ownerId == currentUserId }
                        ?: allGarages.firstOrNull()
                }

                // If still no garage exists in database, create a default partner garage profile for this owner
                if (garage == null) {
                    val defaultGarage = Garage(
                        ownerId = currentUserId,
                        name = "Apex Motor Works & Detailing",
                        description = "Professional automotive detailing studio & care center.",
                        city = "Bangalore",
                        address = "Hosur Main Road, Electronic City",
                        contactNumber = "9876543210",
                        rating = 4.8,
                        reviewCount = 12
                    )
                    val createRes = garageRepository.createGarage(defaultGarage)
                    if (createRes.isSuccess) {
                        garage = defaultGarage
                    }
                }

                currentGarage = garage

                if (garage != null && garage.id.isNotBlank()) {
                    val servicesRes = serviceRepository.getServicesByGarageId(garage.id)
                    if (servicesRes.isSuccess) {
                        servicesList = servicesRes.getOrDefault(emptyList())
                    } else {
                        errorMessage = servicesRes.exceptionOrNull()?.localizedMessage ?: "Failed to load services."
                    }
                } else {
                    servicesList = emptyList()
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "An error occurred while loading services."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadGarageAndServices()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AutoCareTopBar(
                title = "Manage Services",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            GarageBottomBar(
                currentRoute = "manage_services",
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
                        Text(
                            text = "Loading services...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "⚠️", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Unable to load services",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage ?: "Unknown error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            PrimaryButton(
                                text = "Retry",
                                onClick = { loadGarageAndServices() },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    // Header Card
                    item {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "🛠️", fontSize = 26.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentGarage?.name ?: "Workshop Partner",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Active Service Catalog • Live on Marketplace",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        PrimaryButton(
                            text = "+ Add New Service Package",
                            onClick = {
                                serviceBeingEdited = null
                                isDialogVisible = true
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Configured Services (${servicesList.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Instant marketplace sync",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Services List
                    if (servicesList.isEmpty()) {
                        item {
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "📦", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No Services Configured",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Click '+ Add New Service Package' above to create detailing or maintenance packages for customers.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(servicesList) { service ->
                            AppCard(modifier = Modifier.padding(bottom = 12.dp)) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = service.name,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                if (service.category.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Text(
                                                            text = service.category,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (service.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = service.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "⏱ Est. Duration: ${service.durationMinutes} minutes",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (service.isAvailable) {
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                ) {
                                                    Text(
                                                        text = if (service.isAvailable) "Active" else "Paused",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (service.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))
                                        PriceComponent(price = service.price)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Action Buttons: Edit and Delete
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                serviceToDelete = service
                                            }
                                        ) {
                                            Text(
                                                text = "🗑️ Delete",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        OutlinedButton(
                                            onClick = {
                                                serviceBeingEdited = service
                                                isDialogVisible = true
                                            }
                                        ) {
                                            Text(
                                                text = "✏️ Edit Package",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add / Edit Service Dialog
        if (isDialogVisible) {
            val isEditMode = serviceBeingEdited != null
            val currentGarageId = currentGarage?.id ?: "garage_1"

            ServiceFormDialog(
                isEditMode = isEditMode,
                initialService = serviceBeingEdited,
                isSaving = isSaving,
                onDismiss = { isDialogVisible = false },
                onSave = { name, description, price, duration, category, isAvailable ->
                    coroutineScope.launch {
                        isSaving = true
                        try {
                            if (isEditMode) {
                                val updated = serviceBeingEdited!!.copy(
                                    name = name,
                                    description = description,
                                    price = price,
                                    durationMinutes = duration,
                                    category = category,
                                    isAvailable = isAvailable
                                )
                                val res = serviceRepository.updateService(updated)
                                if (res.isSuccess) {
                                    // Update local list immediately
                                    servicesList = servicesList.map { if (it.id == updated.id) updated else it }
                                    isDialogVisible = false
                                    snackbarHostState.showSnackbar("Service \"$name\" updated successfully!")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to update service: ${res.exceptionOrNull()?.localizedMessage}")
                                }
                            } else {
                                val newService = Service(
                                    garageId = currentGarageId,
                                    name = name,
                                    description = description,
                                    price = price,
                                    durationMinutes = duration,
                                    category = category,
                                    isAvailable = isAvailable
                                )
                                val res = serviceRepository.createService(newService)
                                if (res.isSuccess) {
                                    // Reload list from Firestore
                                    val refreshRes = serviceRepository.getServicesByGarageId(currentGarageId)
                                    servicesList = refreshRes.getOrDefault(servicesList + newService)
                                    isDialogVisible = false
                                    snackbarHostState.showSnackbar("New service \"$name\" added successfully!")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to add service: ${res.exceptionOrNull()?.localizedMessage}")
                                }
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
                        } finally {
                            isSaving = false
                        }
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (serviceToDelete != null) {
            val service = serviceToDelete!!
            AlertDialog(
                onDismissRequest = { serviceToDelete = null },
                title = { Text("Delete Service") },
                text = {
                    Text("Are you sure you want to delete \"${service.name}\"? This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val res = serviceRepository.deleteService(service.id)
                                    if (res.isSuccess) {
                                        servicesList = servicesList.filter { it.id != service.id }
                                        snackbarHostState.showSnackbar("Service \"${service.name}\" deleted.")
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to delete service.")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
                                } finally {
                                    serviceToDelete = null
                                }
                            }
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { serviceToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Material 3 Dialog for Adding or Editing a Service Package.
 * Implements thorough field validation (Name non-blank, Price > 0, Duration > 0).
 */
@Composable
private fun ServiceFormDialog(
    isEditMode: Boolean,
    initialService: Service?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, price: Double, duration: Int, category: String, isAvailable: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialService?.name ?: "") }
    var description by remember { mutableStateOf(initialService?.description ?: "") }
    var priceStr by remember { mutableStateOf(initialService?.let { if (it.price > 0) it.price.toString() else "" } ?: "") }
    var durationStr by remember { mutableStateOf(initialService?.let { if (it.durationMinutes > 0) it.durationMinutes.toString() else "" } ?: "60") }
    var category by remember { mutableStateOf(initialService?.category?.ifBlank { "Detailing" } ?: "Detailing") }
    var isAvailable by remember { mutableStateOf(initialService?.isAvailable ?: true) }

    // Validation error states
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var durationError by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Detailing", "Washing", "Interior Spa", "Paint Protection", "Tuning", "General Service")

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = if (isEditMode) "Edit Service Package" else "Add New Service Package",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Service Name (Required)
                Column {
                    CustomTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) nameError = null
                        },
                        label = "Service Name * (e.g. 9H Ceramic Coat)",
                        isError = nameError != null
                    )
                    if (nameError != null) {
                        Text(
                            text = nameError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // 2. Category Selection Chips
                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                // 3. Price (Required, valid double > 0)
                Column {
                    CustomTextField(
                        value = priceStr,
                        onValueChange = {
                            priceStr = it
                            if (it.toDoubleOrNull() != null && it.toDouble() > 0.0) priceError = null
                        },
                        label = "Price in ₹ * (e.g. 1499.00)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = priceError != null
                    )
                    if (priceError != null) {
                        Text(
                            text = priceError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // 4. Duration in Minutes (Required, valid int > 0)
                Column {
                    CustomTextField(
                        value = durationStr,
                        onValueChange = {
                            durationStr = it
                            if (it.toIntOrNull() != null && it.toInt() > 0) durationError = null
                        },
                        label = "Estimated Duration (Minutes) *",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = durationError != null
                    )
                    if (durationError != null) {
                        Text(
                            text = durationError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // 5. Description (Optional)
                CustomTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Procedure Description (optional)"
                )

                // 6. Availability Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active on Marketplace",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Customers can see and book this package",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    var isValid = true

                    if (name.isBlank()) {
                        nameError = "Service name is required"
                        isValid = false
                    }

                    val priceVal = priceStr.toDoubleOrNull()
                    if (priceVal == null || priceVal <= 0.0) {
                        priceError = "Please enter a valid price greater than 0"
                        isValid = false
                    }

                    val durationVal = durationStr.toIntOrNull()
                    if (durationVal == null || durationVal <= 0) {
                        durationError = "Please enter a valid duration in minutes"
                        isValid = false
                    }

                    if (isValid) {
                        onSave(
                            name.trim(),
                            description.trim().ifBlank { "Professional automotive service by certified technicians." },
                            priceVal!!,
                            durationVal!!,
                            category,
                            isAvailable
                        )
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saving...")
                } else {
                    Text(if (isEditMode) "Save Changes" else "Add Package")
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}
