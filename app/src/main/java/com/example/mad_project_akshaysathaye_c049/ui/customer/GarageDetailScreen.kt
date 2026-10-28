package com.example.mad_project_akshaysathaye_c049.ui.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageImageProvider
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageImageType
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.mad_project_akshaysathaye_c049.data.model.Review
import com.example.mad_project_akshaysathaye_c049.data.model.Service
import com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.ReviewRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.ServiceRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.PriceComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.RatingComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.SecondaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun GarageDetailScreen(
    garageId: String,
    onNavigateToServiceDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToBookService: (String, String) -> Unit = { _, _ -> },
    garageRepository: GarageRepository = remember { GarageRepository() },
    serviceRepository: ServiceRepository = remember { ServiceRepository() },
    reviewRepository: ReviewRepository = remember { ReviewRepository() }
) {
    var garage by remember { mutableStateOf<Garage?>(null) }
    var services by remember { mutableStateOf<List<Service>>(emptyList()) }
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }

    var isGarageLoading by remember { mutableStateOf(true) }
    var isServicesLoading by remember { mutableStateOf(true) }
    var isReviewsLoading by remember { mutableStateOf(true) }

    var garageError by remember { mutableStateOf<String?>(null) }
    var servicesError by remember { mutableStateOf<String?>(null) }
    var reviewsError by remember { mutableStateOf<String?>(null) }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Service, 1 = Reviews, 2 = About
    val coroutineScope = rememberCoroutineScope()

    fun loadData() {
        coroutineScope.launch {
            isGarageLoading = true
            garageError = null
            val garageResult = garageRepository.getGarageById(garageId)
            if (garageResult.isSuccess) {
                garage = garageResult.getOrNull()
            } else {
                garageError = garageResult.exceptionOrNull()?.localizedMessage ?: "Failed to load garage details."
            }
            isGarageLoading = false
        }

        coroutineScope.launch {
            isServicesLoading = true
            servicesError = null
            val servicesResult = serviceRepository.getServicesByGarageId(garageId)
            if (servicesResult.isSuccess) {
                services = servicesResult.getOrDefault(emptyList())
            } else {
                servicesError = servicesResult.exceptionOrNull()?.localizedMessage ?: "Failed to load services."
            }
            isServicesLoading = false
        }

        coroutineScope.launch {
            isReviewsLoading = true
            reviewsError = null
            val reviewsResult = reviewRepository.getReviewsByGarageId(garageId)
            if (reviewsResult.isSuccess) {
                reviews = reviewsResult.getOrDefault(emptyList())
            } else {
                reviewsError = reviewsResult.exceptionOrNull()?.localizedMessage ?: "Failed to load reviews."
            }
            isReviewsLoading = false
        }
    }

    LaunchedEffect(garageId) {
        loadData()
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "Garage Details",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            val currentGarage = garage
            val firstService = services.firstOrNull()
            if (currentGarage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Call Action Button (only if valid phone number is available)
                        val hasPhone = currentGarage.contactNumber.isNotBlank()
                        SecondaryButton(
                            text = "Call",
                            onClick = { /* Call action if supported */ },
                            enabled = hasPhone,
                            modifier = Modifier.weight(1f)
                        )

                        // Book Service Action Button: direct handoff to booking flow
                        PrimaryButton(
                            text = "Book Service",
                            onClick = {
                                val sId = firstService?.id ?: ""
                                onNavigateToBookService(currentGarage.id, sId)
                            },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            isGarageLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading workshop details...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            garageError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
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
                                text = "Unable to load garage",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = garageError ?: "Unknown error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            PrimaryButton(
                                text = "Retry",
                                onClick = { loadData() },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }

            garage == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Garage not found.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            else -> {
                val currentGarage = garage!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. Large Hero Image Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .background(Color(0xFF1E293B))
                        ) {
                            // Local Bundled Garage Image with Guaranteed Fallback
                            Image(
                                painter = painterResource(id = GarageImageProvider.getGarageImageRes(currentGarage.id)),
                                contentDescription = currentGarage.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Subtle bottom gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f))
                            )

                            // Visual Tag for Legal Transparency
                            val imgMeta = GarageImageProvider.getMetadata(currentGarage.id)
                            val tagText = when (imgMeta.imageType) {
                                GarageImageType.AI_GENERATED -> "AI Workshop Illustration"
                                GarageImageType.CURATED_WORKSHOP -> "Curated Workshop Photo"
                                GarageImageType.DEFAULT_FALLBACK -> "Partner Workshop"
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = tagText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // 2. Garage Header Details Card (Matching Left Phone in Reference UI)
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentGarage.name.ifBlank { "Motada Auto Service" },
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = currentGarage.address.ifBlank { "Andheri East, Mumbai, Maharashtra 400069" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    val dynamicRating = if (reviews.isNotEmpty()) {
                                        Math.round((reviews.sumOf { it.rating.toDouble() } / reviews.size) * 10.0) / 10.0
                                    } else {
                                        currentGarage.rating
                                    }
                                    val dynamicReviewCount = if (reviews.isNotEmpty()) reviews.size else currentGarage.reviewCount

                                    RatingComponent(
                                        rating = dynamicRating,
                                        reviewCount = dynamicReviewCount
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Tags Row (Distance, Vehicle Types, Operating Hours, Pick & Drop)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp).padding(end = 2.dp)
                                        )
                                        Text(
                                            text = if (currentGarage.locality.isNotBlank()) "${currentGarage.locality}, Mumbai" else "Andheri, Mumbai",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                        )
                                        Text(
                                            text = "Car & Bike",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                        )
                                        Text(
                                            text = currentGarage.operatingHours.ifBlank { "9:00 AM - 8:00 PM" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                        )
                                        Text(
                                            text = "Pick & Drop",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // 3. Segmented Pill Tabs: Service, Reviews, About (Matching Left Phone in Reference UI)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val tabTitles = listOf("Service", "Reviews", "About")
                                    tabTitles.forEachIndexed { index, title ->
                                        val isSelected = selectedTabIndex == index
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .clickable { selectedTabIndex = index }
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Tab Content Section
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            when (selectedTabIndex) {
                                0 -> ServicesTabSection(
                                    garageId = currentGarage.id,
                                    services = services,
                                    isLoading = isServicesLoading,
                                    errorMessage = servicesError,
                                    onNavigateToServiceDetail = onNavigateToServiceDetail,
                                    onNavigateToBookService = onNavigateToBookService,
                                    onRetry = { loadData() }
                                )

                                1 -> ReviewsTabSection(
                                    reviews = reviews,
                                    garageRating = currentGarage.rating,
                                    reviewCount = currentGarage.reviewCount,
                                    isLoading = isReviewsLoading,
                                    errorMessage = reviewsError,
                                    onRetry = { loadData() }
                                )

                                2 -> AboutTabSection(garage = currentGarage)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServicesTabSection(
    garageId: String,
    services: List<Service>,
    isLoading: Boolean,
    errorMessage: String?,
    onNavigateToServiceDetail: (String) -> Unit,
    onNavigateToBookService: (String, String) -> Unit,
    onRetry: () -> Unit
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Loading workshop services...", style = MaterialTheme.typography.bodyMedium)
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
                    Text("Failed to load services", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(14.dp))
                    PrimaryButton(text = "Retry", onClick = onRetry, modifier = Modifier.width(130.dp))
                }
            }
        }

        services.isEmpty() -> {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Services Listed", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This garage has not added service packages yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                services.forEach { service ->
                    AppCard(
                        onClick = { onNavigateToServiceDetail(service.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = service.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (service.category.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = service.category,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (service.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = service.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))
                                PriceComponent(price = service.price)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val durationText = if (service.durationMinutes > 0) {
                                    if (service.durationMinutes >= 60 && service.durationMinutes % 60 == 0) {
                                        "${service.durationMinutes / 60} hr${if (service.durationMinutes / 60 > 1) "s" else ""}"
                                    } else {
                                        "${service.durationMinutes} mins"
                                    }
                                } else {
                                    "60 mins"
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp).padding(end = 4.dp)
                                        )
                                        Text(
                                            text = durationText,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onNavigateToServiceDetail(service.id) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Details", style = MaterialTheme.typography.labelSmall)
                                    }

                                    PrimaryButton(
                                        text = "Book Now",
                                        onClick = { onNavigateToBookService(garageId, service.id) },
                                        modifier = Modifier.height(36.dp).width(105.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Reviews Tab
 * Displays live reviews from Firestore with aggregate rating and individual cards.
 * Calculates average rating directly from the loaded review list for accuracy.
 */
@Composable
private fun ReviewsTabSection(
    reviews: List<Review>,
    garageRating: Double,
    reviewCount: Int,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Loading customer reviews...", style = MaterialTheme.typography.bodyMedium)
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
                    Text("Failed to load reviews", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(14.dp))
                    PrimaryButton(text = "Retry", onClick = onRetry, modifier = Modifier.width(130.dp))
                }
            }
        }

        else -> {
            val dynamicRating = if (reviews.isNotEmpty()) {
                Math.round((reviews.sumOf { it.rating.toDouble() } / reviews.size) * 10.0) / 10.0
            } else {
                garageRating
            }
            val dynamicReviewCount = if (reviews.isNotEmpty()) reviews.size else reviewCount

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", dynamicRating),
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            RatingComponent(rating = dynamicRating)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Verified Customer Reviews",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Based on $dynamicReviewCount verified service reviews.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (reviews.isEmpty()) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No written reviews published yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    reviews.forEach { review ->
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "👤", fontSize = 16.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = review.customerName.ifBlank { "Verified Customer" },
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }

                                    RatingComponent(rating = review.rating.toDouble())
                                }

                                if (review.comment.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = review.comment,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutTabSection(garage: Garage) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading(title = "About Workshop")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = garage.description.ifBlank { "Harish Nagar, Karelibagh, Vadodara, Gujarat 390001" },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        SectionHeading(title = "Operating Hours")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = garage.operatingHours.ifBlank { "9am - 8pm" },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
