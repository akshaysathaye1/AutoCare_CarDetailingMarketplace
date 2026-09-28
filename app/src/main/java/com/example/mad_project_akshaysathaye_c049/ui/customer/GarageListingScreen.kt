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
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomerBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.RatingComponent

@Composable
fun GarageListingScreen(
    onNavigateToGarageDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "Garages Near You",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            CustomerBottomBar(
                currentRoute = "garage_listing",
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
                Text(
                    text = "Select a garage to view services and book",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(3) { index ->
                val garageId = "garage_${index + 1}"
                val garageName = when (index) {
                    0 -> "SpeedAuto Care & Detailing"
                    1 -> "Apex Motor Works"
                    else -> "Elite Shine Studio"
                }
                val address = when (index) {
                    0 -> "MG Road, Central Auto Hub"
                    1 -> "Indiranagar 100ft Road"
                    else -> "Koramangala 5th Block"
                }
                val rating = when (index) {
                    0 -> 4.8
                    1 -> 4.5
                    else -> 4.9
                }

                AppCard(
                    onClick = { onNavigateToGarageDetail(garageId) },
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = garageName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RatingComponent(rating = rating, reviewCount = (index + 1) * 35)
                    }
                }
            }
        }
    }
}
