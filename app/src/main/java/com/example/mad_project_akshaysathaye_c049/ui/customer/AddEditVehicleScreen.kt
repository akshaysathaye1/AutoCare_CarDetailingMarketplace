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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.data.model.Vehicle
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.VehicleRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomTextField
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun AddEditVehicleScreen(
    vehicleId: String? = null,
    onNavigateBack: () -> Unit,
    vehicleRepository: VehicleRepository = remember { VehicleRepository() },
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditMode = !vehicleId.isNullOrBlank()

    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Car") }
    var color by remember { mutableStateOf("") }

    var makeError by remember { mutableStateOf<String?>(null) }
    var modelError by remember { mutableStateOf<String?>(null) }
    var licenseError by remember { mutableStateOf<String?>(null) }
    var yearError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(isEditMode) }
    var isSaving by remember { mutableStateOf(false) }
    var generalError by remember { mutableStateOf<String?>(null) }

    // Load existing vehicle details if in edit mode
    LaunchedEffect(vehicleId) {
        if (isEditMode) {
            isLoading = true
            val currentUserId = authRepository.getEffectiveUserId("CUSTOMER")
            val result = vehicleRepository.getVehicleById(vehicleId!!)
            if (result.isSuccess) {
                val v = result.getOrNull()
                if (v != null) {
                    // Customer ownership check: customers can only access and modify their own vehicles
                    if (currentUserId.isNotBlank() && v.ownerId.isNotBlank() && v.ownerId != currentUserId) {
                        generalError = "Access denied: You can only view and modify your own registered vehicles."
                    } else {
                        make = v.make
                        model = v.model
                        year = v.year
                        licensePlate = v.licensePlate
                        type = if (v.type.isNotBlank()) v.type else "Car"
                        color = v.color
                    }
                } else {
                    generalError = "Vehicle record could not be found."
                }
            } else {
                generalError = result.exceptionOrNull()?.localizedMessage ?: "Failed to load vehicle"
            }
            isLoading = false
        }
    }

    fun validate(): Boolean {
        var isValid = true

        val trimmedMake = make.trim()
        if (trimmedMake.isBlank()) {
            makeError = "Make / Brand is required (e.g. Honda, Hyundai, Tata)"
            isValid = false
        } else if (trimmedMake.length < 2) {
            makeError = "Make must be at least 2 characters long"
            isValid = false
        } else {
            makeError = null
        }

        val trimmedModel = model.trim()
        if (trimmedModel.isBlank()) {
            modelError = "Model name is required (e.g. City, Creta, Harrier)"
            isValid = false
        } else {
            modelError = null
        }

        val cleanedPlate = licensePlate.trim().uppercase()
        if (cleanedPlate.isBlank()) {
            licenseError = "Registration number is required"
            isValid = false
        } else if (!VehicleRepository.isValidRegistrationNumber(cleanedPlate)) {
            licenseError = "Enter a valid registration number (e.g. MH 12 AB 1234 or KA 01 MJ 1234)"
            isValid = false
        } else {
            licenseError = null
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (year.trim().isNotBlank()) {
            val y = year.trim().toIntOrNull()
            if (y == null || y < 1980 || y > currentYear + 1) {
                yearError = "Enter a valid 4-digit year (1980 - ${currentYear + 1})"
                isValid = false
            } else {
                yearError = null
            }
        } else {
            yearError = null
        }

        return isValid
    }

    fun saveVehicle() {
        if (isSaving) return
        if (!validate()) return

        val ownerId = authRepository.getEffectiveUserId("CUSTOMER")
        if (ownerId.isBlank()) {
            generalError = "You must be signed in as a customer to save a vehicle.\nPlease log in or register."
            return
        }

        if (generalError != null && generalError!!.contains("Access denied", ignoreCase = true)) {
            return
        }

        coroutineScope.launch {
            isSaving = true
            generalError = null
            try {
                val vehicleObj = Vehicle(
                    id = vehicleId ?: "",
                    ownerId = ownerId,
                    make = make.trim(),
                    model = model.trim(),
                    year = year.trim(),
                    licensePlate = licensePlate.trim().uppercase(),
                    type = type,
                    color = color.trim()
                )

                val result = if (isEditMode) {
                    vehicleRepository.updateVehicle(vehicleObj, ownerId)
                } else {
                    vehicleRepository.addVehicle(vehicleObj).map { Unit }
                }

                if (result.isSuccess) {
                    val msg = if (isEditMode) "Vehicle updated successfully!" else "Vehicle added successfully!"
                    snackbarHostState.showSnackbar(msg)
                    onNavigateBack()
                } else {
                    val ex = result.exceptionOrNull()
                    val rawMsg = ex?.localizedMessage ?: ""
                    generalError = when {
                        rawMsg.contains("PERMISSION_DENIED", ignoreCase = true) || rawMsg.contains("permission", ignoreCase = true) ->
                            "Database permission denied. You can only save vehicles for your own account."
                        rawMsg.contains("network", ignoreCase = true) || rawMsg.contains("unavailable", ignoreCase = true) ->
                            "Network error. Please check your internet connection."
                        else -> ex?.localizedMessage ?: "Failed to save vehicle"
                    }
                }
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = if (isEditMode) "Edit Vehicle" else "Add New Vehicle",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    Text("Loading vehicle profile...", style = MaterialTheme.typography.bodyMedium)
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
                // Header Card
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = if (type == "Bike") "🏍️" else "🚗", fontSize = 28.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = if (isEditMode) "Update Vehicle Details" else "Register Your Vehicle",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Saved vehicles enable fast 1-click booking at workshops",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error Message if any
                if (generalError != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = generalError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Vehicle Type Selection
                Text(
                    text = "Vehicle Type",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = type == "Car",
                        onClick = { type = "Car" },
                        label = { Text("🚗 Car / SUV / Sedan") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    FilterChip(
                        selected = type == "Bike",
                        onClick = { type = "Bike" },
                        label = { Text("🏍️ Motorcycle / Scooter") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Make & Model
                CustomTextField(
                    value = make,
                    onValueChange = {
                        make = it
                        if (makeError != null) makeError = null
                    },
                    label = "Make / Brand (e.g. Honda, Hyundai, Tata)",
                    isError = makeError != null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                if (makeError != null) {
                    Text(
                        text = makeError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                CustomTextField(
                    value = model,
                    onValueChange = {
                        model = it
                        if (modelError != null) modelError = null
                    },
                    label = "Model (e.g. City, Creta, Harrier)",
                    isError = modelError != null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                if (modelError != null) {
                    Text(
                        text = modelError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // License Plate / Registration
                CustomTextField(
                    value = licensePlate,
                    onValueChange = {
                        licensePlate = it.uppercase()
                        if (licenseError != null) licenseError = null
                    },
                    label = "Registration Number (e.g. KA 01 MJ 1234)",
                    isError = licenseError != null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                if (licenseError != null) {
                    Text(
                        text = licenseError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Year & Color Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        CustomTextField(
                            value = year,
                            onValueChange = {
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    year = it
                                }
                                if (yearError != null) yearError = null
                            },
                            label = "Year (e.g. 2022)",
                            isError = yearError != null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        if (yearError != null) {
                            Text(
                                text = yearError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        CustomTextField(
                            value = color,
                            onValueChange = { color = it },
                            label = "Color (Optional)",
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                PrimaryButton(
                    text = if (isSaving) "Saving..." else if (isEditMode) "Save Changes" else "Add Vehicle",
                    onClick = { saveVehicle() },
                    enabled = !isSaving
                )
            }
        }
    }
}
