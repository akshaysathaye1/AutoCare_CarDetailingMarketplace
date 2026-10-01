package com.example.mad_project_akshaysathaye_c049.ui.customer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.data.model.Garage
import com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.*

@Composable
fun CustomerHomeScreen(
    onNavigateToGarages: () -> Unit,
    onNavigateToGarageDetail: (String) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onLogout: () -> Unit = {},
    garageRepository: GarageRepository = remember { GarageRepository() }
) {
    var searchQuery by remember { mutableStateOf("") }
    var garagesList by remember { mutableStateOf<List<Garage>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }

    val filteredGarages = remember(garagesList, searchQuery) {
        if (searchQuery.isBlank()) garagesList
        else garagesList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.address.contains(searchQuery, ignoreCase = true) ||
            it.city.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) {
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
                    TextButton(onClick = onLogout) {
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

            Spacer(modifier = Modifier.height(18.dp))

            CustomTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "🔍 Search garages by name, address or city",
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Text(text = "✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(20.dp))

            AppCard(modifier = Modifier.fillMaxWidth()) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeading(title = "Recommended Garages (${garagesList.size})")
                TextButton(onClick = onNavigateToGarages) {
                    Text(
                        text = "View All →",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredGarages.isNotEmpty()) {
                filteredGarages.forEach { garage ->
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
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🚘", fontSize = 24.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = garage.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (garage.isOpen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = if (garage.isOpen) "Open" else "Closed",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (garage.isOpen) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

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
            } else if (searchQuery.isNotBlank() && filteredGarages.isEmpty()) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🔍", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Garages Found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No garages match \"$searchQuery\". Try adjusting your search query.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryButton(
                            text = "Reset Search",
                            onClick = { searchQuery = "" }
                        )
                    }
                }
            } else {
                Text("No garages available.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}