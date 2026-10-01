package com.example.mad_project_akshaysathaye_c049.ui.customer

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.data.model.Garage
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomerBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomTextField
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.RatingComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading
import kotlinx.coroutines.launch

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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Filtering states (supported by existing Garage model fields: rating, city, isOpen)
    var selectedMinRating by remember { mutableDoubleStateOf(0.0) }
    var selectedCity by remember { mutableStateOf<String?>(null) }
    var onlyOpenNow by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun loadGarages() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = garageRepository.getAllGarages()
            if (result.isSuccess) {
                garagesList = result.getOrDefault(emptyList())
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load garages"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadGarages()
    }

    // Available unique cities extracted from current garage data
    val availableCities = remember(garagesList) {
        garagesList.map { it.city.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Filtered garages based on Search + Filters combination
    val filteredGarages = remember(garagesList, searchQuery, selectedMinRating, selectedCity, onlyOpenNow) {
        val trimmedQuery = searchQuery.trim().lowercase()
        garagesList.filter { garage ->
            val matchesSearch = trimmedQuery.isEmpty() ||
                    garage.name.lowercase().contains(trimmedQuery) ||
                    garage.address.lowercase().contains(trimmedQuery) ||
                    garage.city.lowercase().contains(trimmedQuery) ||
                    garage.description.lowercase().contains(trimmedQuery)

            val matchesRating = garage.rating >= selectedMinRating
            val matchesCity = selectedCity == null || garage.city.equals(selectedCity, ignoreCase = true)
            val matchesOpen = !onlyOpenNow || garage.isOpen

            matchesSearch && matchesRating && matchesCity && matchesOpen
        }
    }

    val isFilterOrSearchActive = searchQuery.isNotBlank() || selectedMinRating > 0.0 || selectedCity != null || onlyOpenNow
    val activeFilterCount = (if (selectedMinRating > 0.0) 1 else 0) +
            (if (selectedCity != null) 1 else 0) +
            (if (onlyOpenNow) 1 else 0)

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

            // 2. Search Bar with Clear Button
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

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Quick Filter Chips Row (Rating, Location, Open Now, Reset)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter Dialog Trigger Button
                BadgedBox(
                    badge = {
                        if (activeFilterCount > 0) {
                            Badge { Text(activeFilterCount.toString()) }
                        }
                    }
                ) {
                    FilterChip(
                        selected = activeFilterCount > 0,
                        onClick = { showFilterDialog = true },
                        label = { Text("⚡ Filters") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Rating 4.5+ Chip
                FilterChip(
                    selected = selectedMinRating == 4.5,
                    onClick = {
                        selectedMinRating = if (selectedMinRating == 4.5) 0.0 else 4.5
                    },
                    label = { Text("★ 4.5+") }
                )

                // Rating 4.0+ Chip
                FilterChip(
                    selected = selectedMinRating == 4.0,
                    onClick = {
                        selectedMinRating = if (selectedMinRating == 4.0) 0.0 else 4.0
                    },
                    label = { Text("★ 4.0+") }
                )

                // Open Now Chip
                FilterChip(
                    selected = onlyOpenNow,
                    onClick = { onlyOpenNow = !onlyOpenNow },
                    label = { Text("Open Now") }
                )

                // Dynamic City Chips
                availableCities.forEach { city ->
                    FilterChip(
                        selected = selectedCity.equals(city, ignoreCase = true),
                        onClick = {
                            selectedCity = if (selectedCity.equals(city, ignoreCase = true)) null else city
                        },
                        label = { Text("📍 $city") }
                    )
                }

                // Clear All Filters Chip
                if (isFilterOrSearchActive) {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            selectedMinRating = 0.0
                            selectedCity = null
                            onlyOpenNow = false
                        }
                    ) {
                        Text(
                            text = "Reset All",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Loading, Error, Empty & Content States
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Loading garages...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "⚠️", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Unable to load garages",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage ?: "Unknown error occurred.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            PrimaryButton(
                                text = "Retry",
                                onClick = { loadGarages() },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }

                garagesList.isEmpty() -> {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🏢", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Garages Available",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "There are no garages in the marketplace right now.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { loadGarages() }) {
                                Text("Refresh Marketplace")
                            }
                        }
                    }
                }

                isFilterOrSearchActive && filteredGarages.isEmpty() -> {
                    // No Search / Filter matches state
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🔍", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Garages Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    "No garages match \"$searchQuery\" with the selected filters."
                                } else {
                                    "No garages match the selected filters."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            PrimaryButton(
                                text = "Reset Filters & Search",
                                onClick = {
                                    searchQuery = ""
                                    selectedMinRating = 0.0
                                    selectedCity = null
                                    onlyOpenNow = false
                                }
                            )
                        }
                    }
                }

                isFilterOrSearchActive -> {
                    // Search & Filter Results list
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeading(title = "Results (${filteredGarages.size})")
                        Text(
                            text = "Filtered by search & tags",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    filteredGarages.forEach { garage ->
                        GarageItemCard(
                            garage = garage,
                            onClick = { onNavigateToGarageDetail(garage.id) }
                        )
                    }
                }

                else -> {
                    // Default Home Layout (no active search / filter)
                    // 1. Upcoming Booking Placeholder
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

                    // 2. Popular Services Horizontal Row
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

                    // 3. Recommended Garages Section
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

                    Spacer(modifier = Modifier.height(8.dp))

                    garagesList.forEach { garage ->
                        GarageItemCard(
                            garage = garage,
                            onClick = { onNavigateToGarageDetail(garage.id) }
                        )
                    }
                }
            }
        }
    }

    // Comprehensive Filter Dialog
    if (showFilterDialog) {
        var tempRating by remember { mutableDoubleStateOf(selectedMinRating) }
        var tempCity by remember { mutableStateOf(selectedCity) }
        var tempOpenNow by remember { mutableStateOf(onlyOpenNow) }

        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = {
                Text(
                    text = "Filter Garages",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Rating Filter Section
                    Column {
                        Text(
                            text = "Minimum Rating",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val ratings = listOf(0.0 to "Any", 3.5 to "3.5+ ★", 4.0 to "4.0+ ★", 4.5 to "4.5+ ★", 4.8 to "4.8+ ★")
                            ratings.forEach { (r, label) ->
                                FilterChip(
                                    selected = tempRating == r,
                                    onClick = { tempRating = r },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    // City / Location Filter Section
                    if (availableCities.isNotEmpty()) {
                        Column {
                            Text(
                                text = "Location / City",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = tempCity == null,
                                    onClick = { tempCity = null },
                                    label = { Text("All Cities") }
                                )
                                availableCities.forEach { city ->
                                    FilterChip(
                                        selected = tempCity.equals(city, ignoreCase = true),
                                        onClick = { tempCity = city },
                                        label = { Text(city) }
                                    )
                                }
                            }
                        }
                    }

                    // Open Now Toggle Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open Now Only",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Show workshops currently operating",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = tempOpenNow,
                            onCheckedChange = { tempOpenNow = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedMinRating = tempRating
                        selectedCity = tempCity
                        onlyOpenNow = tempOpenNow
                        showFilterDialog = false
                    }
                ) {
                    Text("Apply Filters")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        tempRating = 0.0
                        tempCity = null
                        tempOpenNow = false
                        selectedMinRating = 0.0
                        selectedCity = null
                        onlyOpenNow = false
                        showFilterDialog = false
                    }
                ) {
                    Text("Reset All", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
private fun GarageItemCard(
    garage: Garage,
    onClick: () -> Unit
) {
    AppCard(
        onClick = onClick,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🚘", fontSize = 26.sp)
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
                        color = if (garage.isOpen) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = if (garage.isOpen) "Open" else "Closed",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (garage.isOpen) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
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
