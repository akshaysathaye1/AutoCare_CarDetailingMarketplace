package com.example.mad_project_akshaysathaye_c049.ui.customer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.PriceComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading

@Composable
fun ServiceDetailScreen(
    serviceId: String,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "Service Details",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SectionHeading(title = "Deep Interior Car Detailing")
            Spacer(modifier = Modifier.height(8.dp))
            PriceComponent(price = 1499.0)

            Spacer(modifier = Modifier.height(16.dp))

            AppCard {
                Text(
                    text = "Service Overview",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Includes complete seat shampooing, dashboard UV dressing, floor mat steam cleaning, ceiling dry wash, and AC vent bacterial sanitization.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "⏱ Duration: Approx. 120 minutes\n🛡 Warranty: 30 days shine guarantee",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AppCard {
                Text(
                    text = "Booking Engine Note",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Date/time slot selection and booking will be enabled in Member 3's Booking Engine module.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = "Back to Workshop",
                onClick = onNavigateBack
            )
        }
    }
}
