package com.example.mad_project_akshaysathaye_c049.ui.garage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading

@Composable
fun GarageDashboardScreen(
    onNavigateToServices: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    Scaffold(
        topBar = {
            AutoCareTopBar(title = "Garage Dashboard")
        },
        bottomBar = {
            GarageBottomBar(
                currentRoute = "garage_dashboard",
                onNavigateToRoute = onNavigateToRoute
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SectionHeading(title = "Apex Motor Works 🔧")
            Text(
                text = "Partner Workshop Overview",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                AppCard(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "12",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total Services",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.padding(6.dp))

                AppCard(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "8",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Active Bookings",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeading(title = "Quick Actions")
            Spacer(modifier = Modifier.height(12.dp))

            AppCard(onClick = onNavigateToServices) {
                Text(
                    text = "🛠️ Manage Services & Pricing",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add, edit, or configure detailing packages and rates",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AppCard(onClick = onNavigateToBookings) {
                Text(
                    text = "📅 View Customer Bookings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Review incoming vehicle service appointments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AppCard(onClick = onNavigateToProfile) {
                Text(
                    text = "🏢 Workshop Profile & Timing",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Update address, phone number, and garage photos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
