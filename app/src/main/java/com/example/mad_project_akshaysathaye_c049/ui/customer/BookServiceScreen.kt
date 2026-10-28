package com.example.mad_project_akshaysathaye_c049.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageImageProvider
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TwoWheeler
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.mad_project_akshaysathaye_c049.data.model.Garage
import com.example.mad_project_akshaysathaye_c049.data.model.Service
import com.example.mad_project_akshaysathaye_c049.data.model.Vehicle
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.BookingRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.ServiceRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.VehicleRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.CustomTextField
import com.example.mad_project_akshaysathaye_c049.ui.components.PriceComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.SecondaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DateOption(
    val timestamp: Long,
    val dayOfWeek: String,
    val dayNumber: String,
    val monthName: String,
    val isToday: Boolean
)

@Composable
fun BookServiceScreen(
    garageId: String,
    serviceId: String,
    onNavigateBack: () -> Unit,
    onBookingSuccess: (String) -> Unit,
    onNavigateToAddVehicle: () -> Unit,
    garageRepository: GarageRepository = remember { GarageRepository() },
    serviceRepository: ServiceRepository = remember { ServiceRepository() },
    vehicleRepository: VehicleRepository = remember { VehicleRepository() },
    bookingRepository: BookingRepository = remember { BookingRepository() },
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    val coroutineScope = rememberCoroutineScope()

    var garage by remember { mutableStateOf<Garage?>(null) }
    var service by remember { mutableStateOf<Service?>(null) }
    var availableServices by remember { mutableStateOf<List<Service>>(emptyList()) }
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Booking selections
    var selectedServiceId by remember { mutableStateOf(serviceId) }
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var selectedDateTimestamp by remember { mutableLongStateOf(0L) }
    var selectedTimeSlot by remember { mutableStateOf("") }
    var customerNotes by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(com.example.mad_project_akshaysathaye_c049.data.model.PaymentMethod.PAY_AT_GARAGE) }

    var isSlotConflict by remember { mutableStateOf(false) }
    var isCheckingSlot by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var confirmedBookingId by remember { mutableStateOf<String?>(null) }

    // Generate selectable upcoming 14 days (Today + 13 future days)
    val dateOptions = remember {
        val list = mutableListOf<DateOption>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayNumFormat = SimpleDateFormat("dd", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

        for (i in 0 until 14) {
            val ts = cal.timeInMillis
            val dayOfWeek = if (i == 0) "Today" else dayFormat.format(cal.time)
            val dayNumber = dayNumFormat.format(cal.time)
            val monthName = monthFormat.format(cal.time)
            list.add(DateOption(ts, dayOfWeek, dayNumber, monthName, i == 0))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Default to Today on first load
    LaunchedEffect(Unit) {
        if (selectedDateTimestamp == 0L && dateOptions.isNotEmpty()) {
            selectedDateTimestamp = dateOptions[0].timestamp
        }
    }

    // Standard daily workshop time slots
    val allTimeSlots = listOf(
        "09:00 AM",
        "10:30 AM",
        "12:00 PM",
        "01:30 PM",
        "03:00 PM",
        "04:30 PM",
        "06:00 PM"
    )

    // Check which slots are past if today is selected
    fun isSlotInPast(slot: String, dateTimestamp: Long): Boolean {
        val nowCal = Calendar.getInstance()
        val slotCal = Calendar.getInstance().apply {
            timeInMillis = dateTimestamp
        }

        // Only enforce for today's date
        if (nowCal.get(Calendar.YEAR) != slotCal.get(Calendar.YEAR) ||
            nowCal.get(Calendar.DAY_OF_YEAR) != slotCal.get(Calendar.DAY_OF_YEAR)
        ) {
            return false
        }

        // Parse slot hour and minute
        val parts = slot.split(" ", ":")
        if (parts.size >= 3) {
            var hour = parts[0].toIntOrNull() ?: 0
            val minute = parts[1].toIntOrNull() ?: 0
            val amPm = parts[2]
            if (amPm.equals("PM", ignoreCase = true) && hour < 12) hour += 12
            if (amPm.equals("AM", ignoreCase = true) && hour == 12) hour = 0

            slotCal.set(Calendar.HOUR_OF_DAY, hour)
            slotCal.set(Calendar.MINUTE, minute)
            return slotCal.before(nowCal)
        }
        return false
    }

    // Load initial garage, services, and customer's vehicles
    fun loadInitialData() {
        val customerId = authRepository.getEffectiveUserId("CUSTOMER")
        coroutineScope.launch {
            isLoading = true
            errorMessage = null

            // 1. Garage
            val garageRes = garageRepository.getGarageById(garageId)
            if (garageRes.isSuccess) {
                garage = garageRes.getOrNull()
            }
            if (garage == null) {
                // Fallback to all garages if specific ID missing
                val allG = garageRepository.getAllGarages().getOrDefault(emptyList())
                garage = allG.firstOrNull { it.id == garageId } ?: allG.firstOrNull()
            }

            // 2. Services
            val activeGarageId = garage?.id ?: garageId
            val servicesRes = serviceRepository.getServicesByGarageId(activeGarageId)
            if (servicesRes.isSuccess) {
                availableServices = servicesRes.getOrDefault(emptyList())
            }
            if (availableServices.isNotEmpty()) {
                service = availableServices.firstOrNull { it.id == selectedServiceId }
                    ?: availableServices.first()
                selectedServiceId = service?.id ?: ""
            } else if (selectedServiceId.isNotBlank()) {
                val singleSvc = serviceRepository.getServiceById(selectedServiceId).getOrNull()
                service = singleSvc
            }

            // Fallback default service if database was blank
            if (service == null) {
                service = Service(
                    id = "svc_default",
                    garageId = activeGarageId,
                    name = "Full Body Foam Wash & Wax",
                    description = "Complete exterior wash and paint gloss finish.",
                    price = 799.0,
                    durationMinutes = 60,
                    category = "Washing"
                )
            }

            // 3. Vehicles
            val vehicleRes = vehicleRepository.getVehiclesByOwnerId(customerId)
            if (vehicleRes.isSuccess) {
                vehicles = vehicleRes.getOrDefault(emptyList())
                if (vehicles.isNotEmpty() && selectedVehicle == null) {
                    selectedVehicle = vehicles.first()
                }
            }

            isLoading = false
        }
    }

    LaunchedEffect(garageId, serviceId) {
        loadInitialData()
    }

    // Re-verify slot conflict whenever garage, date, or time slot changes
    LaunchedEffect(selectedDateTimestamp, selectedTimeSlot) {
        if (selectedDateTimestamp > 0L && selectedTimeSlot.isNotBlank() && garage != null) {
            isCheckingSlot = true
            val customerId = authRepository.getEffectiveUserId("CUSTOMER")
            val conflictRes = bookingRepository.checkSlotConflict(
                garageId = garage!!.id,
                scheduledDate = selectedDateTimestamp,
                scheduledTime = selectedTimeSlot,
                customerId = customerId
            )
            isSlotConflict = conflictRes.getOrDefault(false)
            isCheckingSlot = false
        } else {
            isSlotConflict = false
        }
    }

    fun submitBooking() {
        if (isSubmitting) return

        val currentGarage = garage
        val currentService = service
        val currentVehicle = selectedVehicle
        val customerId = authRepository.getEffectiveUserId("CUSTOMER")

        if (customerId.isBlank()) {
            errorMessage = "Please log in to continue booking."
            return
        }
        if (currentGarage == null) {
            errorMessage = "Please select a valid workshop."
            return
        }
        if (currentService == null) {
            errorMessage = "Please select a detailing service."
            return
        }
        if (currentVehicle == null) {
            errorMessage = "Please choose a vehicle for the appointment using '+ Add Vehicle'."
            return
        }
        if (selectedDateTimestamp <= 0L) {
            errorMessage = "Please choose an appointment date."
            return
        }
        if (selectedTimeSlot.isBlank()) {
            errorMessage = "Please choose a time slot for your appointment."
            return
        }
        if (isSlotInPast(selectedTimeSlot, selectedDateTimestamp)) {
            errorMessage = "The selected time slot has already passed for today. Please pick an upcoming slot."
            return
        }
        if (isSlotConflict) {
            errorMessage = "This time slot is already booked. Please choose another slot."
            return
        }

        coroutineScope.launch {
            isSubmitting = true
            errorMessage = null

            // Revalidate availability immediately before confirming to prevent race conditions
            val preCheck = bookingRepository.checkSlotConflict(
                garageId = currentGarage.id,
                scheduledDate = selectedDateTimestamp,
                scheduledTime = selectedTimeSlot,
                customerId = customerId
            )
            if (preCheck.getOrDefault(false)) {
                isSlotConflict = true
                errorMessage = "This slot is already reserved. Please select another time slot."
                isSubmitting = false
                return@launch
            }

            val vehicleDetailsFormatted = "${currentVehicle.make} ${currentVehicle.model} (${currentVehicle.licensePlate})"

            val newBooking = Booking(
                customerId = customerId,
                garageId = currentGarage.id,
                serviceId = currentService.id,
                vehicleId = currentVehicle.id,
                status = "pending",
                scheduledDate = selectedDateTimestamp,
                scheduledTime = selectedTimeSlot,
                totalAmount = currentService.price,
                notes = customerNotes.trim(),
                garageName = currentGarage.name,
                garageAddress = "${currentGarage.address}, ${currentGarage.city}",
                serviceName = currentService.name,
                vehicleDetails = vehicleDetailsFormatted,
                customerName = "Customer",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                paymentMethod = selectedPaymentMethod,
                paymentStatus = if (selectedPaymentMethod == com.example.mad_project_akshaysathaye_c049.data.model.PaymentMethod.ONLINE_TEST) {
                    com.example.mad_project_akshaysathaye_c049.data.model.PaymentStatus.PENDING_VERIFICATION
                } else {
                    com.example.mad_project_akshaysathaye_c049.data.model.PaymentStatus.PENDING
                },
                paymentTransactionId = if (selectedPaymentMethod == com.example.mad_project_akshaysathaye_c049.data.model.PaymentMethod.ONLINE_TEST) {
                    "TEST_TXN_${System.currentTimeMillis()}"
                } else ""
            )

            val result = bookingRepository.createBookingWithAtomicReservation(newBooking)
            isSubmitting = false

            if (result.isSuccess) {
                confirmedBookingId = result.getOrNull()
            } else {
                val ex = result.exceptionOrNull()
                val rawMsg = ex?.localizedMessage ?: ""
                errorMessage = when {
                    rawMsg.contains("SLOT_ALREADY_RESERVED", ignoreCase = true) || rawMsg.contains("already booked", ignoreCase = true) ->
                        "This workshop and time slot has just been reserved by another customer. Please select another slot."
                    rawMsg.contains("PERMISSION_DENIED", ignoreCase = true) || rawMsg.contains("permission", ignoreCase = true) ->
                        "Database access denied. Please verify your account permissions."
                    rawMsg.contains("network", ignoreCase = true) || rawMsg.contains("unavailable", ignoreCase = true) ->
                        "Network connection error. Please check your internet connection."
                    else -> ex?.localizedMessage ?: "Failed to create booking. Please try again."
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "Book Appointment",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading booking options...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Validation Error Alert Banner
                if (errorMessage != null) {
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { errorMessage = null }
                                    .padding(4.dp)
                                    .size(18.dp)
                            )
                        }
                    }
                }

                // 1. Garage & Service Header Summary Card
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Image(
                                painter = painterResource(id = GarageImageProvider.getGarageImageRes(garage?.id)),
                                contentDescription = garage?.name ?: "Workshop",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = garage?.name ?: "Workshop",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = listOfNotNull(garage?.address, garage?.city).filter { it.isNotBlank() }.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Selected Package",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = service?.name ?: "Detailing Service",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Duration: ~${service?.durationMinutes ?: 60} mins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        PriceComponent(price = service?.price ?: 0.0)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error Banner if present
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 2. Select Appointment Date (Horizontal Calendar Cards)
                SectionHeading(title = "1. Choose Date")
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dateOptions) { option ->
                        val isSelected = selectedDateTimestamp == option.timestamp
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            tonalElevation = if (isSelected) 4.dp else 1.dp,
                            modifier = Modifier
                                .width(80.dp)
                                .clickable {
                                    selectedDateTimestamp = option.timestamp
                                    // If slot was past on today, clear selected time slot
                                    if (selectedTimeSlot.isNotBlank() && isSlotInPast(selectedTimeSlot, option.timestamp)) {
                                        selectedTimeSlot = ""
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = option.dayOfWeek,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = option.dayNumber,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = option.monthName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 3. Select Time Slot
                SectionHeading(title = "2. Choose Available Time Slot")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Working hours: 09:00 AM – 07:00 PM",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isCheckingSlot) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Checking slot...", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Time slots grid / wrap
                val chunkedSlots = allTimeSlots.chunked(3)
                chunkedSlots.forEach { rowSlots ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSlots.forEach { slot ->
                            val isPast = isSlotInPast(slot, selectedDateTimestamp)
                            val isSelected = selectedTimeSlot == slot
                            val isConflict = isSelected && isSlotConflict

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (!isPast) {
                                        selectedTimeSlot = slot
                                    }
                                },
                                enabled = !isPast,
                                label = {
                                    Text(
                                        text = if (isPast) "$slot (Past)" else slot,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isConflict) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = if (isConflict) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill extra spacer if row has less than 3
                        if (rowSlots.size < 3) {
                            for (s in 0 until (3 - rowSlots.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (isSlotConflict) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Text(
                            text = "Slot $selectedTimeSlot is already booked for this garage. Please pick another time.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 4. Select Vehicle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeading(title = "3. Select Vehicle")
                    TextButton(onClick = onNavigateToAddVehicle) {
                        Text(
                            text = "+ Add Vehicle",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (vehicles.isEmpty()) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No Vehicles Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Please add your car or motorcycle to finalize this appointment.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SecondaryButton(
                                text = "Add Vehicle Profile",
                                onClick = onNavigateToAddVehicle,
                                modifier = Modifier.height(44.dp)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        vehicles.forEach { vehicle ->
                            val isSelected = selectedVehicle?.id == vehicle.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedVehicle = vehicle }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (vehicle.type.equals("Bike", ignoreCase = true)) Icons.Default.TwoWheeler else Icons.Default.DirectionsCar,
                                        contentDescription = vehicle.type,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${vehicle.make} ${vehicle.model}",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${vehicle.licensePlate} • ${vehicle.type}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 5. Special Instructions / Notes
                SectionHeading(title = "4. Special Instructions (Optional)")
                Spacer(modifier = Modifier.height(8.dp))
                CustomTextField(
                    value = customerNotes,
                    onValueChange = { customerNotes = it },
                    label = "e.g. Focus on interior stain, car keys handover note..."
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 5. Payment Method Selection
                SectionHeading(title = "5. Payment Method")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isGaragePay = selectedPaymentMethod == com.example.mad_project_akshaysathaye_c049.data.model.PaymentMethod.PAY_AT_GARAGE
                    FilterChip(
                        selected = isGaragePay,
                        onClick = { selectedPaymentMethod = com.example.mad_project_akshaysathaye_c049.data.model.PaymentMethod.PAY_AT_GARAGE },
                        label = {
                            Text("💵 Pay at Workshop")
                        },
                        modifier = Modifier.weight(1f)
                    )

                    val isOnlineTest = selectedPaymentMethod == com.example.mad_project_akshaysathaye_c049.data.model.PaymentMethod.ONLINE_TEST
                    FilterChip(
                        selected = isOnlineTest,
                        onClick = { selectedPaymentMethod = com.example.mad_project_akshaysathaye_c049.data.model.PaymentMethod.ONLINE_TEST },
                        label = {
                            Text("💳 Pay Online (Test)")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (selectedPaymentMethod == com.example.mad_project_akshaysathaye_c049.data.model.PaymentMethod.ONLINE_TEST) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🧪 Secure Test Mode",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Generates a simulated payment reference. Real payments require backend signature verification with zero client-side secrets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pay conveniently after service completion via Cash, UPI, or Card at the workshop.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 6. Booking Confirmation Summary Card
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Booking Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val formattedDateStr = if (selectedDateTimestamp > 0L) {
                        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateTimestamp))
                    } else "Not selected"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date & Time:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (selectedTimeSlot.isNotBlank()) "$formattedDateStr • $selectedTimeSlot" else formattedDateStr,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vehicle:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = selectedVehicle?.let { "${it.make} ${it.model} (${it.licensePlate})" } ?: "None selected",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Service Cost:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "₹${String.format("%.2f", service?.price ?: 0.0)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Payment:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (selectedPaymentMethod == com.example.mad_project_akshaysathaye_c049.data.model.PaymentMethod.ONLINE_TEST) "Online (Test Flow)" else "Pay at Workshop",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Payable:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        PriceComponent(price = service?.price ?: 0.0)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Confirmation Button
                PrimaryButton(
                    text = if (isSubmitting) "Confirming Appointment..." else "Confirm & Book Appointment",
                    onClick = { submitBooking() },
                    enabled = !isSubmitting
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Success Confirmation Dialog
    if (confirmedBookingId != null) {
        val bId = confirmedBookingId!!
        AlertDialog(
            onDismissRequest = { /* forces click action */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎉 ", fontSize = 24.sp)
                    Text(
                        text = "Booking Confirmed!",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Your appointment has been successfully created with ${garage?.name ?: "the workshop"}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Booking Reference",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = bId,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val dStr = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date(selectedDateTimestamp))
                            Text(
                                text = "Slot: $dStr at $selectedTimeSlot",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = "View in My Bookings",
                    onClick = {
                        confirmedBookingId = null
                        onBookingSuccess(bId)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}
