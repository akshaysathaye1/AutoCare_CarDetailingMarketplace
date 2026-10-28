package com.example.mad_project_akshaysathaye_c049.ui.garage

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.example.mad_project_akshaysathaye_c049.data.model.BookingStatus
import com.example.mad_project_akshaysathaye_c049.data.model.Garage
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.BookingRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomTextField
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.PriceComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import com.example.mad_project_akshaysathaye_c049.ui.customer.StatusBadge
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GarageBookingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    bookingRepository: BookingRepository = remember { BookingRepository() },
    garageRepository: GarageRepository = remember { GarageRepository() },
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentGarage by remember { mutableStateOf<Garage?>(null) }
    var bookingsList by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") } // All, Pending, Confirmed, In Progress, Completed, Cancelled

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog action state: (booking, newStatus, dialogTitle, prompt, isRejectAction)
    var pendingActionBooking by remember { mutableStateOf<Booking?>(null) }
    var pendingNewStatus by remember { mutableStateOf<String?>(null) }
    var actionDialogTitle by remember { mutableStateOf("") }
    var actionDialogMessage by remember { mutableStateOf("") }
    var rejectReason by remember { mutableStateOf("") }
    var isProcessingAction by remember { mutableStateOf(false) }

    fun loadGarageAndBookings() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val currentUserId = authRepository.getEffectiveUserId("GARAGE")
                var garage: Garage? = null

                if (currentUserId.isNotBlank()) {
                    val garageRes = garageRepository.getGarageByOwnerId(currentUserId)
                    garage = garageRes.getOrNull()

                    // Match strictly by ownerId
                    if (garage == null) {
                        val allGaragesRes = garageRepository.getAllGarages()
                        val allGarages = allGaragesRes.getOrDefault(emptyList())
                        garage = allGarages.firstOrNull { it.ownerId == currentUserId }
                    }
                }

                currentGarage = garage

                if (garage != null && garage.id.isNotBlank()) {
                    val bookingsRes = bookingRepository.getBookingsByGarageId(garage.id)
                    if (bookingsRes.isSuccess) {
                        bookingsList = bookingsRes.getOrDefault(emptyList())
                    } else {
                        errorMessage = bookingsRes.exceptionOrNull()?.localizedMessage ?: "Failed to load bookings."
                    }
                } else {
                    bookingsList = emptyList()
                    if (currentUserId.isNotBlank() && garage == null) {
                        errorMessage = "No registered garage found for this account. Please set up your workshop profile."
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "An error occurred while loading bookings."
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadGarageAndBookings()
    }

    fun executeStatusUpdate(booking: Booking, targetStatus: String) {
        // Enforce garage ownership
        val garage = currentGarage
        if (garage != null && booking.garageId.isNotBlank() && booking.garageId != garage.id) {
            errorMessage = "Unauthorized: You can only update bookings belonging to your workshop."
            return
        }

        // Enforce valid status transition
        if (!BookingStatus.isValidTransition(booking.status, targetStatus)) {
            errorMessage = "Invalid status transition: Cannot change status from '${booking.status}' to '$targetStatus'"
            return
        }

        coroutineScope.launch {
            isProcessingAction = true
            val result = if (targetStatus == "cancelled") {
                bookingRepository.cancelBooking(
                    bookingId = booking.id,
                    cancellationReason = rejectReason.trim().ifEmpty { "Declined by garage workshop" }
                )
            } else {
                bookingRepository.updateBookingStatus(
                    bookingId = booking.id,
                    newStatus = targetStatus
                )
            }
            isProcessingAction = false
            pendingActionBooking = null
            pendingNewStatus = null
            rejectReason = ""

            if (result.isSuccess) {
                val successMessage = when (targetStatus) {
                    "confirmed" -> "Booking confirmed successfully!"
                    "in_progress" -> "Service marked In Progress."
                    "completed" -> "Service marked Completed!"
                    "cancelled" -> "Booking declined."
                    else -> "Status updated."
                }
                snackbarHostState.showSnackbar(successMessage)
                loadGarageAndBookings()
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to update booking status."
            }
        }
    }

    // Filter bookings by selected chip
    val filteredBookings = remember(bookingsList, selectedFilter) {
        when (selectedFilter) {
            "Pending" -> bookingsList.filter { it.status == "pending" }
            "Confirmed" -> bookingsList.filter { it.status == "confirmed" }
            "In Progress" -> bookingsList.filter { it.status == "in_progress" }
            "Completed" -> bookingsList.filter { it.status == "completed" }
            "Cancelled" -> bookingsList.filter { it.status == "cancelled" }
            else -> bookingsList
        }
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "Manage Bookings",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(onClick = { loadGarageAndBookings() }) {
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
            GarageBottomBar(
                currentRoute = "garage_bookings",
                onNavigateToRoute = onNavigateToRoute
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Workshop Header Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentGarage?.name ?: "Partner Workshop",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Queue for ${currentGarage?.city ?: "Active Workshop"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${bookingsList.size} Total",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Filter Chips Row
                    val filterCategories = listOf("All", "Pending", "Confirmed", "In Progress", "Completed", "Cancelled")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filterCategories.forEach { category ->
                            val count = when (category) {
                                "Pending" -> bookingsList.count { it.status == "pending" }
                                "Confirmed" -> bookingsList.count { it.status == "confirmed" }
                                "In Progress" -> bookingsList.count { it.status == "in_progress" }
                                "Completed" -> bookingsList.count { it.status == "completed" }
                                "Cancelled" -> bookingsList.count { it.status == "cancelled" }
                                else -> bookingsList.size
                            }

                            FilterChip(
                                selected = selectedFilter == category,
                                onClick = { selectedFilter = category },
                                label = { Text("$category ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
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
                            Text("Loading customer bookings...", style = MaterialTheme.typography.bodyMedium)
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
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Error Loading Bookings",
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
                                    onClick = { loadGarageAndBookings() },
                                    modifier = Modifier.width(130.dp)
                                )
                            }
                        }
                    }
                }

                filteredBookings.isEmpty() -> {
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
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No $selectedFilter Bookings",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "When customers schedule detailing appointments for your workshop, they will appear in this queue.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(onClick = { loadGarageAndBookings() }) {
                                    Text("Check Again")
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
                        items(filteredBookings, key = { it.id }) { booking ->
                            GarageBookingManagementCard(
                                booking = booking,
                                onAcceptClick = {
                                    pendingActionBooking = booking
                                    pendingNewStatus = "confirmed"
                                    actionDialogTitle = "Accept Booking?"
                                    actionDialogMessage = "Confirm appointment for ${booking.serviceName} on ${formatDate(booking.scheduledDate)} at ${booking.scheduledTime}."
                                },
                                onRejectClick = {
                                    pendingActionBooking = booking
                                    pendingNewStatus = "cancelled"
                                    actionDialogTitle = "Decline Booking?"
                                    actionDialogMessage = "Are you sure you want to decline this appointment request from the customer?"
                                },
                                onStartJobClick = {
                                    pendingActionBooking = booking
                                    pendingNewStatus = "in_progress"
                                    actionDialogTitle = "Start Detailing Job?"
                                    actionDialogMessage = "Mark vehicle ${booking.vehicleDetails} as In-Shop / Under Servicing."
                                },
                                onCompleteJobClick = {
                                    pendingActionBooking = booking
                                    pendingNewStatus = "completed"
                                    actionDialogTitle = "Complete Service?"
                                    actionDialogMessage = "Confirm that detailing service is fully finished and ready for customer collection."
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Action Confirmation Dialog
    if (pendingActionBooking != null && pendingNewStatus != null) {
        val b = pendingActionBooking!!
        val targetStatus = pendingNewStatus!!
        val isDeclining = targetStatus == "cancelled"

        AlertDialog(
            onDismissRequest = { if (!isProcessingAction) pendingActionBooking = null },
            title = {
                Text(actionDialogTitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Column {
                    Text(actionDialogMessage, style = MaterialTheme.typography.bodyMedium)
                    if (isDeclining) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CustomTextField(
                            value = rejectReason,
                            onValueChange = { rejectReason = it },
                            label = "Reason for declining (e.g. Workshop full, slot unavailable)"
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { executeStatusUpdate(b, targetStatus) },
                    enabled = !isProcessingAction
                ) {
                    Text(
                        text = if (isProcessingAction) "Updating..." else if (isDeclining) "Decline" else "Confirm",
                        color = if (isDeclining) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingActionBooking = null },
                    enabled = !isProcessingAction
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GarageBookingManagementCard(
    booking: Booking,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    onStartJobClick: () -> Unit,
    onCompleteJobClick: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        // Top row: ID and Status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ID: #${booking.id.takeLast(8).uppercase()}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            StatusBadge(status = booking.status)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Service & Vehicle details
        Text(
            text = booking.serviceName.ifBlank { "Detailing Package" },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Vehicle: ${booking.vehicleDetails.ifBlank { "Customer Vehicle" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Customer: ${booking.customerName.ifBlank { "Verified Customer" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))

        // Date, Time Slot, and Price
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Scheduled Slot",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${formatDate(booking.scheduledDate)} • ${booking.scheduledTime}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Fee",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PriceComponent(price = booking.totalAmount)
                val payMethodLabel = if (booking.paymentMethod == "online_test") "Online (${booking.paymentStatus})" else "Pay at Workshop"
                Text(
                    text = payMethodLabel,
                    style = MaterialTheme.typography.labelSmall,
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
                    text = "Customer Note: ${booking.notes}",
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
                    text = "Cancellation: ${booking.cancellationReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Contextual action buttons based on status
        when (booking.status) {
            "pending" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRejectClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Decline", color = MaterialTheme.colorScheme.error)
                    }

                    PrimaryButton(
                        text = "Accept & Confirm",
                        onClick = onAcceptClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    )
                }
            }

            "confirmed" -> {
                PrimaryButton(
                    text = "Start Service (Vehicle In-Shop)",
                    onClick = onStartJobClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                )
            }

            "in_progress" -> {
                PrimaryButton(
                    text = "Mark Service Completed",
                    onClick = onCompleteJobClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                )
            }

            "completed" -> {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Completed & Delivered to Customer",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            "cancelled" -> {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Appointment Cancelled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return if (timestamp > 0L) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    } else "Scheduled Date"
}
