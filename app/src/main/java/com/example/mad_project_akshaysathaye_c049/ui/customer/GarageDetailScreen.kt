package com.example.mad_project_akshaysathaye_c049.ui.customer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.PriceComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.RatingComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading

@Composable
fun GarageDetailScreen(
    garageId: String,
    onNavigateToServiceDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    garageRepository: com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository = androidx.compose.runtime.remember { com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository() },
    serviceRepository: com.example.mad_project_akshaysathaye_c049.data.repository.ServiceRepository = androidx.compose.runtime.remember { com.example.mad_project_akshaysathaye_c049.data.repository.ServiceRepository() }
) {
    var garage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.mad_project_akshaysathaye_c049.data.model.Garage?>(null) }
    var services by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<com.example.mad_project_akshaysathaye_c049.data.model.Service>>(emptyList()) }
    var isLoading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

    androidx.compose.runtime.LaunchedEffect(garageId) {
        isLoading = true
        val garageResult = garageRepository.getGarageById(garageId)
        if (garageResult.isSuccess) {
            garage = garageResult.getOrNull()
        }
        val servicesResult = serviceRepository.getServicesByGarageId(garageId)
        if (servicesResult.isSuccess) {
            services = servicesResult.getOrDefault(emptyList())
        }
        isLoading = false
    }
    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "Garage Details",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else if (garage == null) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Garage not found.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                item {
                    SectionHeading(title = garage!!.name)
                    Spacer(modifier = Modifier.height(4.dp))
                    RatingComponent(rating = garage!!.rating, reviewCount = garage!!.reviewCount)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📍 ${garage!!.address}, ${garage!!.city} • 📞 ${garage!!.contactNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = garage!!.description,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeading(title = "Available Services")
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (services.isEmpty()) {
                    item {
                        Text("No services available.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    items(services.size) { index ->
                        val service = services[index]
                        AppCard(
                            onClick = { onNavigateToServiceDetail(service.id) },
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
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
                                        text = "Category: ${service.category} • Approx. ${service.durationMinutes} mins",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                PriceComponent(price = service.price)
                            }
                        }
                    }
                }
            }
        }
    }
}
