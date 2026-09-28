package com.example.mad_project_akshaysathaye_c049.ui.garage

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
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.PriceComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton

@Composable
fun ManageServicesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
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
                    text = "+ Add New Service",
                    onClick = { /* Member 2 will connect form */ }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Active Services List",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            val garageServices = listOf(
                Pair("Foam Wash & Interior Vacuum", 699.0),
                Pair("Engine Bay Cleaning & Dressing", 899.0),
                Pair("Complete Ceramic Paint Protection", 11999.0),
                Pair("Wheel Balancing & Tire Alignment", 1200.0)
            )

            items(garageServices.size) { index ->
                val (serviceName, price) = garageServices[index]
                AppCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = serviceName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Duration: 60-120 min • Active status: Live",
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
