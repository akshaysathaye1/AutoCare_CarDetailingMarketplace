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
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "Garage Details",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
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
                SectionHeading(title = "SpeedAuto Care Workshop")
                Spacer(modifier = Modifier.height(4.dp))
                RatingComponent(rating = 4.8, reviewCount = 142)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📍 123 Car Street, Bangalore • 📞 +91 9876543210",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Professional automotive detailing studio specializing in ceramic coating, deep interior dry-cleaning, paint protection film (PPF), and general maintenance.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))
                SectionHeading(title = "Available Services")
                Spacer(modifier = Modifier.height(12.dp))
            }

            val services = listOf(
                Triple("srv_1", "Deep Interior Car Detailing", 1499.0),
                Triple("srv_2", "Exterior Foam Wash & Wax", 799.0),
                Triple("srv_3", "Ceramic Coating 9H Pro", 9999.0)
            )

            items(services.size) { index ->
                val (serviceId, title, price) = services[index]
                AppCard(
                    onClick = { onNavigateToServiceDetail(serviceId) },
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Approx. 90 mins • 100% satisfaction guarantee",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PriceComponent(price = price)
                    }
                }
            }
        }
    }
}
