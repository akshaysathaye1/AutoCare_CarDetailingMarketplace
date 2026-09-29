package com.example.mad_project_akshaysathaye_c049.ui.customer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomerBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomTextField
import com.example.mad_project_akshaysathaye_c049.ui.components.RatingComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading

@Composable
fun CustomerHomeScreen(
    onNavigateToGarages: () -> Unit,
    onNavigateToGarageDetail: (String) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onLogout: () -> Unit = {},
    garageRepository: com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository = remember { com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository() }
) {
    var searchQuery by remember { mutableStateOf("") }
    var garagesList by remember { mutableStateOf<List<com.example.mad_project_akshaysathaye_c049.data.model.Garage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        isLoading = true
        val result = garageRepository.getAllGarages()
        if (result.isSuccess) {
            garagesList = result.getOrDefault(emptyList())
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "AutoCare",
                actions = {
                    androidx.compose.material3.TextButton(onClick = onLogout) {
                        Text(
                            text = "Logout",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            )
        },
        bottomBar = {
            CustomerBottomBar(
                currentRoute = "customer_home",
                onNavigateToRoute = onNavigateToRoute
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. User Greeting & Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Good day, Car Enthusiast! 👋",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Find the best garages & detailing studios nearby",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Search Bar
            CustomTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "🔍 Search garages, services (e.g. PPF, Foam Wash)"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Upcoming Booking Placeholder
            AppCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UPCOMING SERVICE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Tomorrow, 10:30 AM",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ceramic 9H Coating & Full Interior Spa",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Garage: SpeedAuto Detailing Hub (MG Road)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Popular Services Horizontal Row
            SectionHeading(title = "Popular Detailing & Services")
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val popularServices = listOf(
                    Triple("Foam Wash", "🚿", "From ₹499"),
                    Triple("Ceramic Coat", "✨", "From ₹6,999"),
                    Triple("Interior Spa", "🧼", "From ₹1,299"),
                    Triple("PPF Wrap", "🛡️", "From ₹14,999"),
                    Triple("Engine Tuning", "⚡", "From ₹2,499")
                )

                popularServices.forEach { (name, icon, price) ->
                    Card(
                        modifier = Modifier.width(130.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = icon, fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = price,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. Featured Garages Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeading(title = "Recommended Garages")
                Text(
                    text = "View All →",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val garages = listOf(
                Triple("SpeedAuto Detailing Studio", "4.8", "MG Road • 2.5 km away"),
                Triple("Apex Motor Works & Tuning", "4.9", "Indiranagar • 4.1 km away"),
                Triple("AutoShine Express Workshop", "4.6", "Koramangala • 5.0 km away")
            )

            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (garagesList.isNotEmpty()) {
                garagesList.take(3).forEach { garage ->
                    AppCard(
                        onClick = { onNavigateToGarageDetail(garage.id) },
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "🚘", fontSize = 24.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = garage.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${garage.address}, ${garage.city}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                RatingComponent(rating = garage.rating, reviewCount = garage.reviewCount)
                            }
                        }
                    }
                }
            } else {
                Text("No garages available.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
