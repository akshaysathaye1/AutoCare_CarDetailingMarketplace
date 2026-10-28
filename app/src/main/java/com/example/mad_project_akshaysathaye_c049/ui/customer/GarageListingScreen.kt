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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageImageProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.data.model.Garage
import com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomerBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.RatingComponent
import kotlinx.coroutines.launch

@Composable
fun GarageListingScreen(
    initialCategory: String? = null,
    onNavigateToGarageDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    garageRepository: GarageRepository = remember { GarageRepository() }
) {
    var garagesList by remember { mutableStateOf<List<Garage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(initialCategory) }
    var onlyAvailable by remember { mutableStateOf(false) }
    var pickupAndDrop by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun loadGarages() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = garageRepository.getAllGarages()
            if (result.isSuccess) {
                garagesList = result.getOrDefault(emptyList())
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load garages."
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadGarages()
    }

    val filteredGarages = remember(garagesList, searchQuery, selectedCategory, onlyAvailable, pickupAndDrop) {
        val query = searchQuery.trim().lowercase()
        garagesList.filter { garage ->
            val matchesSearch = query.isEmpty() ||
                    garage.name.lowercase().contains(query) ||
                    garage.address.lowercase().contains(query) ||
                    garage.city.lowercase().contains(query) ||
                    garage.description.lowercase().contains(query)

            val matchesCategory = matchesMarketplaceCategory(garage, selectedCategory)
            val matchesAvailable = !onlyAvailable || garage.isOpen
            val matchesPickup = !pickupAndDrop ||
                    garage.description.contains("pickup", ignoreCase = true) ||
                    garage.description.contains("drop", ignoreCase = true)

            matchesSearch && matchesCategory && matchesAvailable && matchesPickup
        }
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = if (!selectedCategory.isNullOrBlank()) "$selectedCategory Garages" else "Search Garages",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Search Input and Filter Row
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search garages, location, or service...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clickable { searchQuery = "" }
                                        .padding(8.dp)
                                        .size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Filter Chips Row (GoMechanic Categories + Toggles)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val categoryOptions = listOf(
                            "All",
                            "Car Servicing",
                            "Oil & Filter",
                            "AC Service",
                            "Brakes",
                            "Battery",
                            "Tyres",
                            "Wheel Alignment",
                            "Denting & Painting",
                            "Washing",
                            "Detailing"
                        )

                        categoryOptions.forEach { cat ->
                            val isSelected = if (cat == "All") selectedCategory == null else selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = if (cat == "All" || isSelected) null else cat
                                },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }

                        // Available Toggle Chip
                        FilterChip(
                            selected = onlyAvailable,
                            onClick = { onlyAvailable = !onlyAvailable },
                            label = { Text("Available Now") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )

                        // Pickup & drop Toggle Chip
                        FilterChip(
                            selected = pickupAndDrop,
                            onClick = { pickupAndDrop = !pickupAndDrop },
                            label = { Text("Pickup & Drop") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Searching available workshops...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Unable to load search results",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = errorMessage ?: "Unknown error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                PrimaryButton(
                                    text = "Retry Search",
                                    onClick = { loadGarages() },
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }
                    }
                }

                // Differentiated State: Live Cloud Firestore database is completely empty
                garagesList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
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
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Marketplace Initializing",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No partner workshops are currently listed in Cloud Firestore.\n\nTo create a legitimate garage listing:\n1. Log out or register a new partner account with role 'GARAGE'.\n2. Open the Garage Dashboard and complete your workshop profile & address.\n3. Add service packages under 'Manage Services'.\n4. Your workshop will appear here live for customers.",
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
                                        onClick = { loadGarages() },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = onNavigateBack,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Back")
                                    }
                                }
                            }
                        }
                    }
                }

                // Differentiated State: Database has garages, but search or category filters yielded 0 results
                filteredGarages.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
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
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No Garages Match Filters",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No workshops match your current search '${searchQuery.trim()}' or category '${selectedCategory ?: "All"}'. Try clearing filters.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(onClick = {
                                    searchQuery = ""
                                    selectedCategory = null
                                    onlyAvailable = false
                                    pickupAndDrop = false
                                }) {
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        items(filteredGarages) { garage ->
                            GarageDiscoveryCard(
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
fun GarageDiscoveryCard(
    garage: Garage,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Large Workshop Image Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color(0xFF1E293B))
            ) {
                // Local Bundled Garage Image with Guaranteed Fallback
                Image(
                    painter = painterResource(id = GarageImageProvider.getGarageImageRes(garage.id)),
                    contentDescription = garage.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle dark scrim so status badges and bookmark stand out
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )

                // Status Tag (Open / Closed)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (garage.isOpen) Color(0xFF065F46) else Color(0xFF475569),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (garage.isOpen) "● Open Now" else "● Closed",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

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

            // Workshop Info Details Section
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${garage.locality.ifBlank { garage.address.ifBlank { "Andheri East" } }}, Mumbai",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (garage.isVerified) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Verified",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (garage.isDemoData) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Demo",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (garage.operatingHours.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Hours",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp).padding(end = 4.dp)
                        )
                        Text(
                            text = garage.operatingHours,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    PrimaryButton(
                        text = "Book Service",
                        onClick = onClick,
                        modifier = Modifier.width(140.dp)
                    )
                }
            }
        }
    }
}
