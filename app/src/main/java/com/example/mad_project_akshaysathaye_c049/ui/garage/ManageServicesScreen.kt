package com.example.mad_project_akshaysathaye_c049.ui.garage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.data.model.Service
import com.example.mad_project_akshaysathaye_c049.data.repository.ServiceRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.*

@Composable
fun ManageServicesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    serviceRepository: ServiceRepository = remember { ServiceRepository() }
) {
    var servicesList by remember { mutableStateOf<List<Service>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var isDialogVisible by remember { mutableStateOf(false) }
        // Connected to Firestore createService

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
                    text = "+ Add New Service Package",
                    onClick = { isDialogVisible = true }
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Configured Services (${servicesList.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}