package com.example.mad_project_akshaysathaye_c049.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomTextField
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.PriceComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton

data class ServiceItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val durationMinutes: Int
)

@Composable
fun ManageServicesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val services = remember {
        mutableStateListOf(
            ServiceItem(
                id = "1",
                name = "Deep Interior Foam & Steam Clean",
                description = "Deep sanitization of upholstery, ceiling, mats & AC vents",
                price = 1499.0,
                durationMinutes = 90
            ),
            ServiceItem(
                id = "2",
                name = "Exterior Snow Foam Wash & Wax",
                description = "High-pressure wash, microfiber drying and carnauba wax coat",
                price = 699.0,
                durationMinutes = 45
            ),
            ServiceItem(
                id = "3",
                name = "Ceramic 9H Coating (3-Year Protection)",
                description = "Twin-layer Japanese 9H ceramic coating with paint correction",
                price = 9999.0,
                durationMinutes = 240
            ),
            ServiceItem(
                id = "4",
                name = "Engine Bay Degreasing & Dressing",
                description = "Removes accumulated grease and restores black plastic trims",
                price = 799.0,
                durationMinutes = 40
            )
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var newServiceName by remember { mutableStateOf("") }
    var newServiceDesc by remember { mutableStateOf("") }
    var newServicePrice by remember { mutableStateOf("") }
    var newServiceDuration by remember { mutableStateOf("") }

    Scaffold(
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                PrimaryButton(
                    text = "+ Add New Service Package",
                    onClick = { showAddDialog = true }
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configured Services (${services.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Active on Marketplace",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(services) { service ->
                AppCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = service.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = service.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⏱ Est. Duration: ${service.durationMinutes} minutes",
                                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.secondary)
                            )
                        }
                        PriceComponent(price = service.price)
                    }
                }
            }
        }

        // Add Service Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(text = "Add Service Package") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomTextField(
                            value = newServiceName,
                            onValueChange = { newServiceName = it },
                            label = "Service Name"
                        )
                        CustomTextField(
                            value = newServiceDesc,
                            onValueChange = { newServiceDesc = it },
                            label = "Description"
                        )
                        CustomTextField(
                            value = newServicePrice,
                            onValueChange = { newServicePrice = it },
                            label = "Price (₹)"
                        )
                        CustomTextField(
                            value = newServiceDuration,
                            onValueChange = { newServiceDuration = it },
                            label = "Duration (Minutes)"
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val priceVal = newServicePrice.toDoubleOrNull() ?: 499.0
                            val durationVal = newServiceDuration.toIntOrNull() ?: 60
                            if (newServiceName.isNotBlank()) {
                                services.add(
                                    ServiceItem(
                                        id = (services.size + 1).toString(),
                                        name = newServiceName,
                                        description = newServiceDesc.ifBlank { "Standard automobile detailing procedure" },
                                        price = priceVal,
                                        durationMinutes = durationVal
                                    )
                                )
                                newServiceName = ""
                                newServiceDesc = ""
                                newServicePrice = ""
                                newServiceDuration = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Add to List")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
