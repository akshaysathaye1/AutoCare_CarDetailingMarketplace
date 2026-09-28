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

@Composable
fun GarageBookingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "Garage Bookings",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            GarageBottomBar(
                currentRoute = "garage_bookings",
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
                    text = "Customer Bookings Queue",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Realtime status updates will be handled by Member 3 (Booking Engine).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            val mockBookings = listOf(
                Triple("BK-1029", "Honda City (KA 01 MJ 1234)", "Ceramic Coating • Oct 5, 10:00 AM"),
                Triple("BK-1030", "Hyundai Creta (KA 05 AB 9988)", "Deep Interior Detailing • Oct 6, 2:00 PM"),
                Triple("BK-1031", "Tata Harrier (KA 03 XY 4567)", "Foam Wash & Wax • Oct 7, 11:30 AM")
            )

            items(mockBookings.size) { index ->
                val (bookingId, car, details) = mockBookings[index]
                AppCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bookingId,
                                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = car,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Confirmed",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
