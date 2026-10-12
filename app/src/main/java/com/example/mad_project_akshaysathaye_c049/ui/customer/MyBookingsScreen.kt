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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.mad_project_akshaysathaye_c049.data.model.Booking
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.BookingRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomerBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomTextField
import com.example.mad_project_akshaysathaye_c049.ui.components.PriceComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MyBookingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGarages: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    bookingRepository: BookingRepository = remember { BookingRepository() },
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    var bookingsList by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Upcoming, 1 = History

    // Cancellation dialog state
    var bookingToCancel by remember { mutableStateOf<Booking?>(null) }
    var cancellationReason by remember { mutableStateOf("") }
    var isCancelling by remember { mutableStateOf(false) }

    fun loadBookings() {
        val customerId = authRepository.getEffectiveUserId("CUSTOMER")
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = bookingRepository.getBookingsByCustomerId(customerId)
            if (result.isSuccess) {
                bookingsList = result.getOrDefault(emptyList())
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load bookings"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadBookings()
    }

    fun confirmCancellation(booking: Booking) {
        coroutineScope.launch {
            isCancelling = true
            val res = bookingRepository.cancelBooking(
                bookingId = booking.id,
                cancellationReason = cancellationReason.trim().ifEmpty { "Customer requested cancellation" }
            )
            isCancelling = false
            bookingToCancel = null
            cancellationReason = ""
            if (res.isSuccess) {
                loadBookings()
            } else {
                errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to cancel booking"
            }
        }
    }

    // Split bookings into Upcoming vs Past/History
    val upcomingBookings = remember(bookingsList) {
        bookingsList.filter { it.status in listOf("pending", "confirmed", "in_progress") }
    }
    val pastBookings = remember(bookingsList) {
        bookingsList.filter { it.status in listOf("completed", "cancelled") }
    }

    val activeList = if (selectedTab == 0) upcomingBookings else pastBookings

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "My Bookings",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(onClick = { loadBookings() }) {
                        Text(
                            text = "Refresh",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            )
        },
        bottomBar = {
            CustomerBottomBar(
                currentRoute = "customer_bookings",
                onNavigateToRoute = onNavigateToRoute
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs: Upcoming (count) vs Past (count)
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Upcoming (${upcomingBookings.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "History (${pastBookings.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading your appointments...", style = MaterialTheme.typography.bodyMedium)
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
                                Text(text = "⚠️", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Failed to load bookings",
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
                                    onClick = { loadBookings() },
                                    modifier = Modifier.width(130.dp)
                                )
                            }
                        }
                    }
                }

                activeList.isEmpty() -> {
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
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = if (selectedTab == 0) "📅" else "📦", fontSize = 44.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (selectedTab == 0) "No Upcoming Appointments" else "No Booking History",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (selectedTab == 0) {
                                        "Explore top-rated detailing studios and schedule your next car wash or ceramic coating."
                                    } else {
                                        "Completed and cancelled appointments will be archived here."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selectedTab == 0) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    PrimaryButton(
                                        text = "Browse Garages",
                                        onClick = onNavigateToGarages
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(activeList, key = { it.id }) { booking ->
                            BookingCardItem(
                                booking = booking,
                                onCancelClick = {
                                    bookingToCancel = booking
                                    cancellationReason = ""
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Cancellation Dialog
    if (bookingToCancel != null) {
        val b = bookingToCancel!!
        AlertDialog(
            onDismissRequest = { if (!isCancelling) bookingToCancel = null },
            title = {
                Text("Cancel Appointment?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to cancel your booking for ${b.serviceName} at ${b.garageName}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(
                        value = cancellationReason,
                        onValueChange = { cancellationReason = it },
                        label = "Reason for cancellation (optional)"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { confirmCancellation(b) },
                    enabled = !isCancelling
                ) {
                    Text(
                        text = if (isCancelling) "Cancelling..." else "Yes, Cancel Booking",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { bookingToCancel = null },
                    enabled = !isCancelling
                ) {
                    Text("Keep Appointment")
                }
            }
        )
    }
}

@Composable
fun BookingCardItem(
    booking: Booking,
    onCancelClick: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        // Status row & ID
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${booking.id.takeLast(8).uppercase()}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            StatusBadge(status = booking.status)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Service & Workshop
        Text(
            text = booking.serviceName.ifBlank { "Detailing Service" },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "🏢 ${booking.garageName.ifBlank { "AutoCare Workshop" }}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (booking.garageAddress.isNotBlank()) {
            Text(
                text = "📍 ${booking.garageAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(10.dp))

        // Vehicle & Schedule
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vehicle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = booking.vehicleDetails.ifBlank { "Registered Vehicle" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Appointment Slot",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val dateStr = if (booking.scheduledDate > 0L) {
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(booking.scheduledDate))
                } else "Scheduled"
                Text(
                    text = "$dateStr • ${booking.scheduledTime}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (booking.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Note: ${booking.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (booking.status == "cancelled" && booking.cancellationReason.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancellation reason: ${booking.cancellationReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Price & Cancellation Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PriceComponent(price = booking.totalAmount)
            }

            // Cancellation permitted only for pending or confirmed bookings
            if (booking.status in listOf("pending", "confirmed")) {
                OutlinedButton(
                    onClick = onCancelClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        text = "Cancel Booking",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else if (booking.status == "in_progress") {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "🚗 Service In Progress",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            } else if (booking.status == "completed") {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "✓ Service Completed",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (label, bgColor, textColor) = when (status.lowercase()) {
        "pending" -> Triple("Pending Confirmation", Color(0xFFFFF3E0), Color(0xFFE65100))
        "confirmed" -> Triple("Confirmed", Color(0xFFE3F2FD), Color(0xFF1565C0))
        "in_progress" -> Triple("In Progress", Color(0xFFF3E5F5), Color(0xFF6A1B9A))
        "completed" -> Triple("Completed", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "cancelled" -> Triple("Cancelled", Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Triple(status.replaceFirstChar { it.uppercase() }, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
