package com.example.mad_project_akshaysathaye_c049.ui.customer

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.data.model.Booking
import com.example.mad_project_akshaysathaye_c049.data.model.CustomerDashboardStats
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.CustomerDashboardRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomerBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.PriceComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CustomerDashboardScreen — Member 4 implementation.
 *
 * Displays real data for the authenticated customer:
 *  - Profile greeting
 *  - Stats cards: completed services, total spending, upcoming bookings, vehicles
 *  - Upcoming appointments list
 *  - Recent booking history
 *  - Registered vehicles summary
 *  - Quick navigation actions
 *
 * All statistics are computed from Firestore via CustomerDashboardRepository.
 * Cancelled bookings are excluded from service counts and spending totals.
 */
@Composable
fun CustomerDashboardScreen(
    onNavigateToBookings: () -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToGarages: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onLogout: () -> Unit = {},
    dashboardRepository: CustomerDashboardRepository = remember { CustomerDashboardRepository() },
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<CustomerDashboardStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val userId = authRepository.getEffectiveUserId()

    fun loadDashboard() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = dashboardRepository.getDashboardStats(userId)
            if (result.isSuccess) {
                stats = result.getOrNull()
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage
                    ?: "Failed to load dashboard data."
            }
            isLoading = false
        }
    }

    LaunchedEffect(userId) { loadDashboard() }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "My Dashboard",
                actions = {
                    TextButton(onClick = { loadDashboard() }) {
                        Text(
                            text = "Refresh",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
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
                currentRoute = "customer_dashboard",
                onNavigateToRoute = onNavigateToRoute
            )
        }
    ) { paddingValues ->

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading your dashboard...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            errorMessage != null -> {
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
                            Text(text = "⚠️", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Could not load dashboard",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            PrimaryButton(text = "Retry", onClick = { loadDashboard() })
                        }
                    }
                }
            }

            else -> {
                val s = stats ?: CustomerDashboardStats()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // -- 1. Profile Greeting & Summary -----------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(30.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "👤", fontSize = 28.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome back!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Your AutoCare Summary",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // -- Onboarding State for New Customers ------------------
                    if (s.completedServiceCount == 0 && s.upcomingBookingCount == 0 && s.registeredVehicleCount == 0) {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "🚗 Welcome to AutoCare!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Start by adding your vehicle profile or browse verified workshops to book your car detailing services.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PrimaryButton(
                                        text = "+ Add Vehicle",
                                        onClick = onNavigateToVehicles,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PrimaryButton(
                                        text = "Explore Garages",
                                        onClick = onNavigateToGarages,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // -- 2. Stats Cards ---------------------------------------
                    SectionHeading(title = "Account Overview")
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        DashStatCard(
                            label = "Services Done",
                            value = s.completedServiceCount.toString(),
                            icon = "✅",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        DashStatCard(
                            label = "Upcoming",
                            value = s.upcomingBookingCount.toString(),
                            icon = "📅",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AppCard(modifier = Modifier.weight(1f)) {
                            Text(text = "💳", fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            PriceComponent(price = s.totalSpending)
                            Text(
                                text = "Total Spending",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        DashStatCard(
                            label = "Vehicles",
                            value = s.registeredVehicleCount.toString(),
                            icon = "🚗",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // -- 3. Upcoming Appointments -----------------------------
                    SectionHeading(title = "Upcoming Appointments")
                    Spacer(modifier = Modifier.height(10.dp))

                    if (s.upcomingBookings.isEmpty()) {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "📅", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No upcoming appointments",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Browse garages to book your next service.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                PrimaryButton(
                                    text = "Find Garages",
                                    onClick = onNavigateToGarages,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        s.upcomingBookings.take(3).forEach { booking ->
                            MiniBookingCard(booking = booking)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (s.upcomingBookings.size > 3) {
                            TextButton(
                                onClick = onNavigateToBookings,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "View all ${s.upcomingBookings.size} upcoming bookings →",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // -- 4. Recent Bookings -----------------------------------
                    SectionHeading(title = "Recent Activity")
                    Spacer(modifier = Modifier.height(10.dp))

                    if (s.recentBookings.isEmpty()) {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "📝", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No bookings yet",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    } else {
                        s.recentBookings.forEach { booking ->
                            MiniBookingCard(booking = booking)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        TextButton(
                            onClick = onNavigateToBookings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "View all bookings →",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // -- 5. Vehicles Summary ----------------------------------
                    SectionHeading(title = "My Vehicles")
                    Spacer(modifier = Modifier.height(10.dp))

                    if (s.vehicles.isEmpty()) {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "🚗", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No vehicles registered",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                PrimaryButton(
                                    text = "Add a Vehicle",
                                    onClick = onNavigateToVehicles,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        s.vehicles.take(3).forEach { vehicle ->
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (vehicle.type == "Bike") "🏍️" else "🚗",
                                        fontSize = 24.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = vehicle.licensePlate.ifBlank { vehicle.color },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val serviceCount = s.vehicleServiceCounts[vehicle.id] ?: 0
                                        Text(
                                            text = if (serviceCount > 0) "$serviceCount service${if (serviceCount > 1) "s" else ""} completed" else "No completed services",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (serviceCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (s.vehicles.size > 3) {
                            TextButton(
                                onClick = onNavigateToVehicles,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "View all ${s.vehicles.size} vehicles →",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // -- 6. Service History Breakdown -------------------------
                    if (s.serviceHistory.isNotEmpty() || s.spendingByGarage.isNotEmpty()) {
                        SectionHeading(title = "Service History Breakdown")
                        Spacer(modifier = Modifier.height(10.dp))

                        if (s.serviceHistory.isNotEmpty()) {
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Top Requested Services",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    s.serviceHistory.entries.take(4).forEach { (serviceName, count) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = serviceName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "$count service${if (count > 1) "s" else ""}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        if (s.spendingByGarage.isNotEmpty()) {
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Spending by Workshop",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    s.spendingByGarage.entries.take(3).forEach { (garageName, spent) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = garageName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            PriceComponent(price = spent)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // -- 7. Quick Navigation Actions --------------------------
                    SectionHeading(title = "Quick Actions")
                    Spacer(modifier = Modifier.height(10.dp))

                    QuickActionCard(icon = "🔧", label = "Find Garages", sub = "Browse available workshops", onClick = onNavigateToGarages)
                    Spacer(modifier = Modifier.height(8.dp))
                    QuickActionCard(icon = "📅", label = "My Bookings", sub = "View and manage appointments", onClick = onNavigateToBookings)
                    Spacer(modifier = Modifier.height(8.dp))
                    QuickActionCard(icon = "🚗", label = "My Vehicles", sub = "Add or edit your vehicles", onClick = onNavigateToVehicles)

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// -- Private helper composables -----------------------------------------------

@Composable
private fun DashStatCard(label: String, value: String, icon: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Text(text = icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MiniBookingCard(booking: Booking) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = booking.serviceName.ifBlank { "Service" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = booking.garageName.ifBlank { "Workshop" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val dateStr = if (booking.scheduledDate > 0L) {
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(booking.scheduledDate))
                } else "—"
                Text(
                    text = "$dateStr • ${booking.scheduledTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .padding(horizontal = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when (booking.status) {
                    "completed" -> MaterialTheme.colorScheme.secondaryContainer
                    "confirmed" -> MaterialTheme.colorScheme.primaryContainer
                    "cancelled" -> MaterialTheme.colorScheme.errorContainer
                    "in_progress" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = booking.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(icon: String, label: String, sub: String, onClick: () -> Unit) {
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = "→", style = MaterialTheme.typography.titleLarge)
        }
    }
}
