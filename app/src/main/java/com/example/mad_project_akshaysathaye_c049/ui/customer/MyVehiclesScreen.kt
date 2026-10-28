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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.data.model.Vehicle
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.VehicleRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomerBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.SecondaryButton
import kotlinx.coroutines.launch

@Composable
fun MyVehiclesScreen(
    onNavigateToAddEditVehicle: (String?) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    vehicleRepository: VehicleRepository = remember { VehicleRepository() },
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    var vehiclesList by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Deletion dialog state
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    fun loadVehicles() {
        val ownerId = authRepository.getEffectiveUserId("CUSTOMER")
        if (ownerId.isBlank()) {
            isLoading = false
            errorMessage = "Please log in to view your registered vehicles."
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = vehicleRepository.getVehiclesByOwnerId(ownerId)
            if (result.isSuccess) {
                vehiclesList = result.getOrDefault(emptyList())
            } else {
                val ex = result.exceptionOrNull()
                val rawMsg = ex?.localizedMessage ?: ""
                errorMessage = when {
                    rawMsg.contains("PERMISSION_DENIED", ignoreCase = true) || rawMsg.contains("permission", ignoreCase = true) ->
                        "Database access denied. Please verify your account permissions."
                    rawMsg.contains("network", ignoreCase = true) || rawMsg.contains("unavailable", ignoreCase = true) ->
                        "Network connection error. Please check your internet connection."
                    else -> ex?.localizedMessage ?: "Failed to load vehicles"
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadVehicles()
    }

    fun deleteVehicle(vehicle: Vehicle) {
        val ownerId = authRepository.getEffectiveUserId("CUSTOMER")
        if (ownerId.isNotBlank() && vehicle.ownerId.isNotBlank() && vehicle.ownerId != ownerId) {
            errorMessage = "Unauthorized: You can only delete your own registered vehicles."
            vehicleToDelete = null
            return
        }

        coroutineScope.launch {
            isDeleting = true
            val result = vehicleRepository.deleteVehicle(vehicle.id, ownerId)
            isDeleting = false
            vehicleToDelete = null
            if (result.isSuccess) {
                loadVehicles()
            } else {
                val ex = result.exceptionOrNull()
                val rawMsg = ex?.localizedMessage ?: ""
                errorMessage = when {
                    rawMsg.contains("PERMISSION_DENIED", ignoreCase = true) || rawMsg.contains("permission", ignoreCase = true) ->
                        "Permission denied: You can only delete vehicles belonging to your own account."
                    rawMsg.contains("network", ignoreCase = true) || rawMsg.contains("unavailable", ignoreCase = true) ->
                        "Network error. Please check your internet connection."
                    else -> ex?.localizedMessage ?: "Failed to delete vehicle"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "My Vehicles",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(onClick = { onNavigateToAddEditVehicle(null) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Vehicle",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            CustomerBottomBar(
                currentRoute = "my_vehicles",
                onNavigateToRoute = onNavigateToRoute
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddEditVehicle(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Vehicle",
                    modifier = Modifier.size(24.dp)
                )
            }
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
                        Text("Loading your vehicles...", style = MaterialTheme.typography.bodyMedium)
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
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Unable to load vehicles",
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
                                onClick = { loadVehicles() },
                                modifier = Modifier.width(130.dp)
                            )
                        }
                    }
                }
            }

            vehiclesList.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Vehicles Registered",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add your car or motorcycle once to book detailing and workshop appointments with a single tap.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            PrimaryButton(
                                text = "Add Your First Vehicle",
                                onClick = { onNavigateToAddEditVehicle(null) }
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Registered Vehicles (${vehiclesList.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Manage profiles",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(vehiclesList, key = { it.id }) { vehicle ->
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (vehicle.type.equals("Bike", ignoreCase = true)) Icons.Default.TwoWheeler else Icons.Default.DirectionsCar,
                                            contentDescription = vehicle.type,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${vehicle.make} ${vehicle.model}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = vehicle.type,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Registration plate display
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = vehicle.licensePlate,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (vehicle.year.isNotBlank() || vehicle.color.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = listOfNotNull(
                                                if (vehicle.year.isNotBlank()) "Year: ${vehicle.year}" else null,
                                                if (vehicle.color.isNotBlank()) "Color: ${vehicle.color}" else null
                                            ).joinToString(" • "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { vehicleToDelete = vehicle },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                    )
                                    Text(
                                        text = "Delete",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                OutlinedButton(
                                    onClick = { onNavigateToAddEditVehicle(vehicle.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                    )
                                    Text(
                                        text = "Edit",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (vehicleToDelete != null) {
        val v = vehicleToDelete!!
        AlertDialog(
            onDismissRequest = { if (!isDeleting) vehicleToDelete = null },
            title = {
                Text("Delete Vehicle Profile?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Text(
                    "Are you sure you want to remove ${v.make} ${v.model} (${v.licensePlate}) from your garage?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { deleteVehicle(v) },
                    enabled = !isDeleting
                ) {
                    Text(
                        text = if (isDeleting) "Deleting..." else "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { vehicleToDelete = null },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
