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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.data.model.Booking
import com.example.mad_project_akshaysathaye_c049.data.model.Review
import com.example.mad_project_akshaysathaye_c049.data.model.ReviewValidator
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.BookingRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.ReviewRepository
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.PrimaryButton
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading
import com.example.mad_project_akshaysathaye_c049.ui.components.StarRatingInput
import kotlinx.coroutines.launch

/**
 * SubmitReviewScreen allows an authenticated customer to rate and review a
 * completed booking. Eligibility is checked before the form is displayed:
 *
 *  1. The booking must exist and belong to the current user.
 *  2. The booking must have status == "completed".
 *  3. No previous review for this bookingId+customerId pair must exist
 *     (enforced both client-side and by Firestore rules).
 *
 * On successful submission the garage rating is denormalised via
 * ReviewRepository.updateGarageRatingStats().
 */
@Composable
fun SubmitReviewScreen(
    bookingId: String,
    onNavigateBack: () -> Unit,
    onReviewSubmitted: () -> Unit,
    bookingRepository: BookingRepository = remember { BookingRepository() },
    reviewRepository: ReviewRepository = remember { ReviewRepository() },
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var booking by remember { mutableStateOf<Booking?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var eligibilityError by remember { mutableStateOf<String?>(null) }
    var isAlreadyReviewed by remember { mutableStateOf(false) }

    var starRating by remember { mutableIntStateOf(0) }
    var commentText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val currentUserId = authRepository.getEffectiveUserId()

    // Load booking and check eligibility on first composition
    LaunchedEffect(bookingId) {
        isLoading = true
        eligibilityError = null

        if (currentUserId.isBlank()) {
            eligibilityError = "You must be signed in to submit a review."
            isLoading = false
            return@LaunchedEffect
        }

        val bookingResult = bookingRepository.getBookingById(bookingId)
        if (bookingResult.isFailure) {
            eligibilityError = "Could not load booking details. Please try again."
            isLoading = false
            return@LaunchedEffect
        }

        val loadedBooking = bookingResult.getOrNull()
        if (loadedBooking == null) {
            eligibilityError = "Booking not found."
            isLoading = false
            return@LaunchedEffect
        }

        if (loadedBooking.customerId != currentUserId) {
            eligibilityError = "You can only review your own bookings."
            isLoading = false
            return@LaunchedEffect
        }

        if (loadedBooking.status != "completed") {
            eligibilityError = "Reviews can only be submitted for completed services. " +
                    "Current status: ${loadedBooking.status}."
            isLoading = false
            return@LaunchedEffect
        }

        val duplicateResult = reviewRepository.checkReviewExistsForBooking(currentUserId, bookingId)
        if (duplicateResult.isSuccess && duplicateResult.getOrNull() == true) {
            isAlreadyReviewed = true
        }

        booking = loadedBooking
        isLoading = false
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "Write a Review",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            eligibilityError != null -> {
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
                            Text(text = "??", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Cannot Submit Review",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = eligibilityError!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            PrimaryButton(text = "Go Back", onClick = onNavigateBack)
                        }
                    }
                }
            }

            isAlreadyReviewed -> {
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
                            Text(text = "?", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Review Already Submitted",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You have already reviewed this booking. Thank you for your feedback!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            PrimaryButton(text = "Go Back", onClick = onNavigateBack)
                        }
                    }
                }
            }

            else -> {
                val b = booking!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Booking Summary Card
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "??", fontSize = 24.sp)
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(
                                        text = b.garageName.ifBlank { "Workshop" },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = b.serviceName.ifBlank { "Service" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Vehicle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = b.vehicleDetails.ifBlank { "—" },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "? Completed",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Star Rating
                    SectionHeading(title = "Your Rating")
                    StarRatingInput(
                        currentRating = starRating,
                        onRatingChange = { starRating = it; validationError = null }
                    )

                    // Comment Input
                    SectionHeading(title = "Your Review")
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it; validationError = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        placeholder = {
                            Text(
                                text = "Share your experience — quality of work, professionalism, value for money...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        isError = validationError != null,
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (validationError != null) {
                                    Text(
                                        text = validationError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(0.dp))
                                }
                                Text(
                                    text = "${commentText.length}/500",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    // Submit Button
                    Spacer(modifier = Modifier.height(8.dp))
                    PrimaryButton(
                        text = if (isSubmitting) "Submitting..." else "Submit Review",
                        onClick = {
                            if (isSubmitting) return@PrimaryButton
                            val review = Review(
                                bookingId = bookingId,
                                garageId = b.garageId,
                                customerId = currentUserId,
                                customerName = b.customerName.ifBlank { "Verified Customer" },
                                rating = starRating.toFloat(),
                                comment = commentText.trim()
                            )
                            val error = ReviewValidator.validate(review)
                            if (error != null) {
                                validationError = error
                                return@PrimaryButton
                            }
                            isSubmitting = true
                            coroutineScope.launch {
                                val result = reviewRepository.addReview(review)
                                if (result.isSuccess) {
                                    // Denormalise rating stats on the garage document
                                    reviewRepository.updateGarageRatingStats(b.garageId)
                                    onReviewSubmitted()
                                } else {
                                    isSubmitting = false
                                    snackbarHostState.showSnackbar(
                                        message = result.exceptionOrNull()?.localizedMessage
                                            ?: "Failed to submit review. Please try again."
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
