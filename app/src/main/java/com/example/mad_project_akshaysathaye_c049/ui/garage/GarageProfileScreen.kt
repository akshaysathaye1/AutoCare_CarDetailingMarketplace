package com.example.mad_project_akshaysathaye_c049.ui.garage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.RatingComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading

@Composable
fun GarageProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "Workshop Profile",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            GarageBottomBar(
                currentRoute = "garage_profile",
                onNavigateToRoute = onNavigateToRoute
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeading(title = "Apex Motor Works")
            Spacer(modifier = Modifier.height(4.dp))
            RatingComponent(rating = 4.8, reviewCount = 85)

            Spacer(modifier = Modifier.height(20.dp))

            AppCard {
                Text(
                    text = "Business Details",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📍 Address: 45 Industrial Layout, Hosur Road, Bangalore\n\n📞 Phone: +91 98450 12345\n\n⏰ Working Hours: 9:00 AM – 8:00 PM (Mon - Sat)\n\n✉️ Email: contact@apexmotors.in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AppCard {
                Text(
                    text = "Workshop Description",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We provide high-grade professional car detailing, ceramic coating, paint restoration, and periodic maintenance with certified mechanics and genuine spares.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
