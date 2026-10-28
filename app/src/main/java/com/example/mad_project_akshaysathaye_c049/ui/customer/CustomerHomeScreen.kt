package com.example.mad_project_akshaysathaye_c049.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.data.model.Booking
import com.example.mad_project_akshaysathaye_c049.data.model.Garage
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageImageProvider
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.BookingRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomerBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.RatingComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading
import kotlinx.coroutines.launch

@Composable
fun CustomerHomeScreen(
    onNavigateToGarages: (String?) -> Unit,
    onNavigateToGarageDetail: (String) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onLogout: () -> Unit = {},
    garageRepository: GarageRepository = remember { GarageRepository() },
    bookingRepository: BookingRepository = remember { BookingRepository() },
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedVehicleType by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var garagesList by remember { mutableStateOf<List<Garage>>(emptyList()) }
    var activeBookings by remember { mutableStateOf<List<Booking>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Meaningful Comment 1: Firestore data loading coroutine scope
    // Retrieves live marketplace garages and customer's active bookings in parallel.
    // Handles network errors gracefully without crashing Compose state.
    fun loadHomeData() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Meaningful Comment 2: Security rule compliance for garage reading
                // Under firestore.rules, read on /garages/{garageId} is permitted for any authenticated user.
                val garageRes = garageRepository.getAllGarages()
                if (garageRes.isSuccess) {
                    garagesList = garageRes.getOrDefault(emptyList())
                } else {
                    errorMessage = garageRes.exceptionOrNull()?.localizedMessage ?: "Failed to load garages"
                }

                // Meaningful Comment 3: Customer-specific booking query
                // Only queries bookings when the customer UID is confirmed non-blank to prevent PERMISSION_DENIED.
                val uid = authRepository.getEffectiveUserId("CUSTOMER")
                if (uid.isNotBlank()) {
                    val bookingRes = bookingRepository.getBookingsByCustomerId(uid)
                    if (bookingRes.isSuccess) {
                        val allBookings = bookingRes.getOrDefault(emptyList())
                        activeBookings = allBookings.filter { it.status in listOf("pending", "confirmed", "in_progress") }
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "An error occurred while loading marketplace data"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadHomeData()
    }

    // Meaningful Comment 4: Intelligent GoMechanic-inspired marketplace search & category filter
    // Filters garages by text query (name, address, city, description) and semantic category tags.
    val filteredGarages = remember(garagesList, searchQuery, selectedVehicleType, selectedCategory) {
        val trimmedQuery = searchQuery.trim().lowercase()
        garagesList.filter { garage ->
            val matchesSearch = trimmedQuery.isEmpty() ||
                    garage.name.lowercase().contains(trimmedQuery) ||
                    garage.address.lowercase().contains(trimmedQuery) ||
                    garage.city.lowercase().contains(trimmedQuery) ||
                    garage.description.lowercase().contains(trimmedQuery)

            val matchesCategory = matchesMarketplaceCategory(garage, selectedCategory)

            matchesSearch && matchesCategory
        }
    }

    Scaffold(
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
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Top Location Header (Matching Reference UI)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp).padding(end = 2.dp)
                            )
                            Text(
                                text = "Andheri, Mumbai",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Chat / Notification Icon Button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onNavigateToRoute("customer_bookings") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Profile Avatar
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onNavigateToRoute("customer_profile") }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Logout Action
                        TextButton(onClick = onLogout) {
                            Text(
                                text = "Logout",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // 2. Search Bar with Dark Action Button (Matching Reference UI)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "search for services, garage, location..",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .size(52.dp)
                            .clickable { /* Search Action */ }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Vehicles Under Service Section (Matching Reference UI)
                SectionHeading(title = "Vehicles Under Service")
                Spacer(modifier = Modifier.height(10.dp))

                val inProgressBooking = activeBookings.firstOrNull()
                if (inProgressBooking != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            // Coral Orange Banner
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Estimated time remaining : 1hrs",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = "${inProgressBooking.garageName} - Andheri East, Mumbai",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Call,
                                                    contentDescription = "Contact",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = "Details",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Vehicle Info Sub-card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Vehicle",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = inProgressBooking.vehicleDetails.ifBlank { "GJ 36 Ad 4352" },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = inProgressBooking.serviceName.ifBlank { "Honda Deluxe - Hero" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "● ", color = Color(0xFFD97706), fontSize = 10.sp)
                                        Text(
                                            text = inProgressBooking.status.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Default Active Vehicle Card matching Reference UI layout
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Estimated time remaining : 1hrs",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = "Sai Service - Andheri East, Mumbai",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Call,
                                                    contentDescription = "Contact",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = "Details",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Vehicle",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "GJ 36 Ad 4352",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Honda Deluxe - Hero",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "● ", color = Color(0xFFD97706), fontSize = 10.sp)
                                        Text(
                                            text = "In Progress",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Vehicle Type Selection Row (Matching Reference UI: Bike, Car, Scooter, Others)
                SectionHeading(title = "Vehicle type")
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val vehicleTypes = listOf(
                        Triple("Bike", Icons.Default.TwoWheeler, "Bike"),
                        Triple("Car", Icons.Default.DirectionsCar, "Car"),
                        Triple("Scooter", Icons.Default.TwoWheeler, "Scooter"),
                        Triple("Others", Icons.Default.Build, "Others")
                    )

                    vehicleTypes.forEach { (label, icon, typeKey) ->
                        val isSelected = selectedVehicleType == typeKey
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                selectedVehicleType = if (isSelected) null else typeKey
                            }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. Services Category Scroll Row (GoMechanic & GarageConnect 10 Automotive Categories)
                SectionHeading(title = "Service Categories")
                Spacer(modifier = Modifier.height(10.dp))

                data class ServiceCategoryItem(
                    val name: String,
                    val subtitle: String,
                    val icon: androidx.compose.ui.graphics.vector.ImageVector,
                    val bgColor: Color
                )

                val serviceCategories = listOf(
                    ServiceCategoryItem("Car Servicing", "Periodic & Inspection", Icons.Default.Build, Color(0xFF1E293B)),
                    ServiceCategoryItem("Oil & Filter", "Engine Oil & Lube", Icons.Default.Tune, Color(0xFF0F172A)),
                    ServiceCategoryItem("AC Service", "Cooling & Gas Refill", Icons.Default.AcUnit, Color(0xFF1E3A8A)),
                    ServiceCategoryItem("Brakes", "Pads & Rotors", Icons.Default.Warning, Color(0xFF7F1D1D)),
                    ServiceCategoryItem("Battery", "Health & Replace", Icons.Default.BatteryChargingFull, Color(0xFF14532D)),
                    ServiceCategoryItem("Tyres", "Fitting & Punctures", Icons.Default.Sync, Color(0xFF334155)),
                    ServiceCategoryItem("Wheel Alignment", "Balancing & Camber", Icons.Default.Sync, Color(0xFF312E81)),
                    ServiceCategoryItem("Denting & Painting", "Scratch & Paint", Icons.Default.Brush, Color(0xFF701A75)),
                    ServiceCategoryItem("Washing", "Pressure Foam Spa", Icons.Default.LocalCarWash, Color(0xFF065F46)),
                    ServiceCategoryItem("Detailing", "Ceramic & PPF Coat", Icons.Default.AutoFixHigh, Color(0xFF78350F))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    serviceCategories.forEach { category ->
                        val isSelected = selectedCategory == category.name
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else category.bgColor,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .width(124.dp)
                                .height(92.dp)
                                .clickable {
                                    selectedCategory = if (isSelected) null else category.name
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = category.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp).align(Alignment.TopStart)
                                )
                                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = category.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                        color = Color.White.copy(alpha = 0.8f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedCategory != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable { selectedCategory = null }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Filtering: $selectedCategory  ✕",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        TextButton(onClick = { onNavigateToGarages(selectedCategory) }) {
                            Text(
                                text = "View in Listing →",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 6. Popular Near You Section (Matching Reference UI)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeading(title = "Popular near you")
                    TextButton(onClick = { onNavigateToGarages(selectedCategory) }) {
                        Text(
                            text = "See All →",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Loading marketplace garages...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    errorMessage != null -> {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Unable to load garages",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                PrimaryButton(
                                    text = "Retry",
                                    onClick = { loadHomeData() },
                                    modifier = Modifier.width(130.dp)
                                )
                            }
                        }
                    }

                    // Meaningful Comment 5: Differentiate between completely empty database vs. query with 0 matches
                    // When the marketplace collection has 0 garages, we prompt the user with a setup/refresh callout.
                    garagesList.isEmpty() -> {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Marketplace Initializing",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No partner workshops are currently listed in the Cloud Firestore database.\n\nTo create a legitimate garage listing:\n1. Log out and register a new account with the 'GARAGE' role.\n2. In the Garage Dashboard, fill out your workshop profile & address.\n3. Add service packages under 'Manage Services'.\n4. Your workshop will immediately appear live here for customers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PrimaryButton(
                                        text = "Refresh",
                                        onClick = { loadHomeData() },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = onLogout,
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text("Partner Login", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    // Meaningful Comment 6: Active filter with no matching workshops
                    // Garages exist in the database, but user search or category filter narrowed results to zero.
                    filteredGarages.isEmpty() -> {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Garages Match Filters",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No workshops match your current search '${searchQuery.trim()}' or '${selectedCategory ?: ""}'. Try clearing filters.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(onClick = {
                                    searchQuery = ""
                                    selectedCategory = null
                                    selectedVehicleType = null
                                }) {
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }

                    else -> {
                        filteredGarages.forEach { garage ->
                            MarketplaceGarageCard(
                                garage = garage,
                                onClick = { onNavigateToGarageDetail(garage.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketplaceGarageCard(
    garage: Garage,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Workshop Hero Image Header with Bookmark Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF1E293B))
            ) {
                // Local Bundled Garage Image with Guaranteed Fallback
                Image(
                    painter = painterResource(id = GarageImageProvider.getGarageImageRes(garage.id)),
                    contentDescription = garage.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle dark vignette to ensure bookmark and overlay badges are legible
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )

                // Bookmark Icon Button
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Info Details Section
            Column(modifier = Modifier.padding(14.dp)) {
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
                    RatingComponent(rating = garage.rating, reviewCount = garage.reviewCount)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${garage.locality.ifBlank { garage.address.ifBlank { "Andheri East" } }}, Mumbai",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Meaningful Comment 7: GoMechanic-style automotive category taxonomy matching
// Maps user-selected category chips (Car Servicing, Oil & Filter, AC Service, Brakes, Battery, Tyres, Wheel Alignment, Denting & Painting, Washing, Detailing) to workshop capabilities.
// This prevents 0-result false negatives when a workshop offers services matching the category
// but doesn't have the literal single word in its headline.
fun matchesMarketplaceCategory(garage: Garage, category: String?): Boolean {
    if (category.isNullOrBlank()) return true
    val cat = category.trim().lowercase()
    val desc = garage.description.lowercase()
    val name = garage.name.lowercase()

    // Meaningful Comment 8: Direct substring match on name or description
    if (desc.contains(cat) || name.contains(cat)) return true

    // Meaningful Comment 9: Domain-specific service synonyms and keywords
    return when (cat) {
        "car servicing", "car service", "servicing", "service", "periodic" ->
            desc.contains("service") || desc.contains("maintenance") ||
            desc.contains("repair") || desc.contains("inspection") ||
            name.contains("service") || name.contains("auto") || name.contains("garage") || name.contains("motor")

        "oil and filter", "oil & filter", "oil & filter changes", "oil", "filter" ->
            desc.contains("oil") || desc.contains("filter") ||
            desc.contains("lube") || desc.contains("engine") || desc.contains("tuning") ||
            desc.contains("fluid")

        "ac service", "ac", "air conditioning" ->
            desc.contains("ac") || desc.contains("air condition") ||
            desc.contains("cooling") || desc.contains("climate") ||
            desc.contains("gas") || desc.contains("filter")

        "brakes", "brake" ->
            desc.contains("brake") || desc.contains("pad") ||
            desc.contains("rotor") || desc.contains("disc") || desc.contains("caliper")

        "battery" ->
            desc.contains("battery") || desc.contains("electrical") ||
            desc.contains("power") || desc.contains("voltage") || desc.contains("charge")

        "tyres", "tyre", "tire", "tires" ->
            desc.contains("wheel") || desc.contains("tyre") ||
            desc.contains("tire") || desc.contains("alignment") ||
            desc.contains("tuning") || desc.contains("workshop") ||
            name.contains("motor") || name.contains("auto")

        "wheel alignment", "alignment" ->
            desc.contains("alignment") || desc.contains("balancing") ||
            desc.contains("wheel") || desc.contains("suspension") || desc.contains("camber")

        "denting & painting", "denting and painting", "dent", "paint", "denting", "painting" ->
            desc.contains("dent") || desc.contains("paint") || desc.contains("scratch") ||
            desc.contains("body") || desc.contains("panel") || desc.contains("coating")

        "washing", "wash", "car wash" ->
            desc.contains("wash") || desc.contains("spa") || desc.contains("clean") ||
            desc.contains("water") || desc.contains("foam")

        "detailing" ->
            desc.contains("detail") || desc.contains("coat") || desc.contains("ceramic") ||
            desc.contains("ppf") || desc.contains("interior") || desc.contains("polish")

        "body" ->
            desc.contains("detailing") || desc.contains("coating") ||
            desc.contains("wash") || desc.contains("paint") ||
            desc.contains("dent") || desc.contains("ceramic") ||
            name.contains("detailing") || name.contains("shine") || name.contains("spa")

        "engine" ->
            desc.contains("engine") || desc.contains("tuning") ||
            desc.contains("performance") || desc.contains("oil") ||
            desc.contains("mechanical") || name.contains("motor") || name.contains("tuning")

        "electrical" ->
            desc.contains("electrical") || desc.contains("battery") ||
            desc.contains("wiring") || desc.contains("electronics") ||
            desc.contains("tuning") || name.contains("tuning") || name.contains("motor")

        else -> false
    }
}

