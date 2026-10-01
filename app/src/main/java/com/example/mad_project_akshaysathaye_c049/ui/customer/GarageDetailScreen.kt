package com.example.mad_project_akshaysathaye_c049.ui.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun GarageDetailScreen(
    garageId: String,
    onNavigateToServiceDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    garageRepository: GarageRepository = remember { GarageRepository() }
) {
    var garage by remember { mutableStateOf<Garage?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(garageId) {
        isLoading = true
        val garageResult = garageRepository.getGarageById(garageId)
        if (garageResult.isSuccess) {
            garage = garageResult.getOrNull()
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = garage?.name ?: "Garage Details",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (garage == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Garage not found.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            val currentGarage = garage!!
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                AppCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = currentGarage.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "📍 ${currentGarage.address}, ${currentGarage.city}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    RatingComponent(rating = currentGarage.rating, reviewCount = currentGarage.reviewCount)
                }

                val tabs = listOf("Services", "Reviews", "Info")
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    when (selectedTabIndex) {
                        0 -> Text("Services tab")
                        1 -> Text("Reviews tab")
                        2 -> Text("Info tab")
                    }
                }
            }
        }
    }
}