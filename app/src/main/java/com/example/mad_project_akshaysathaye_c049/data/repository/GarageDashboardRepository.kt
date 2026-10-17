package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.Booking
import com.example.mad_project_akshaysathaye_c049.data.model.Garage
import com.example.mad_project_akshaysathaye_c049.data.model.GarageDashboardStats
import com.example.mad_project_akshaysathaye_c049.data.model.Review
import com.example.mad_project_akshaysathaye_c049.data.model.Service
import java.util.Calendar

/**
 * GarageDashboardRepository retrieves and aggregates operational data
 * strictly scoped to the authenticated garage owner.
 *
 * Responsibilities:
 *  - Enforce ownership checking using ownerId against garage.ownerId.
 *  - Aggregate real booking counts by status (pending, confirmed, in_progress, completed).
 *  - Calculate completed service revenue and today's earnings.
 *  - Fetch active listed services and customer reviews.
 *  - All calculations are purely data-driven without hardcoded values.
 */
class GarageDashboardRepository(
    private val garageRepository: GarageRepository = GarageRepository(),
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val serviceRepository: ServiceRepository = ServiceRepository(),
    private val reviewRepository: ReviewRepository = ReviewRepository()
) {

    /**
     * Fetch complete aggregated statistics for the garage dashboard, scoped to [ownerId].
     */
    suspend fun getDashboardStats(ownerId: String): Result<GarageDashboardStats> {
        return try {
            if (ownerId.isBlank()) {
                return Result.failure(IllegalArgumentException("Authentication required: ownerId cannot be blank."))
            }

            // 1. Retrieve the garage owned by this partner account
            val garageResult = garageRepository.getGarageByOwnerId(ownerId)
            if (garageResult.isFailure) {
                return Result.failure(garageResult.exceptionOrNull()!!)
            }

            val garage = garageResult.getOrNull()
            if (garage == null) {
                // Partner has not registered or linked a garage yet
                return Result.success(GarageDashboardStats(garage = null))
            }

            // 2. Verify strict ownership to prevent unauthorized access
            if (garage.ownerId != ownerId) {
                return Result.failure(SecurityException("Unauthorized: User does not own garage ${garage.id}."))
            }

            // 3. Retrieve bookings, services, and reviews for this garage
            val bookingsResult = bookingRepository.getBookingsByGarageId(garage.id)
            val servicesResult = serviceRepository.getServicesByGarageId(garage.id)
            val reviewsResult = reviewRepository.getReviewsByGarageId(garage.id)

            val bookings: List<Booking> = bookingsResult.getOrDefault(emptyList())
            val services: List<Service> = servicesResult.getOrDefault(emptyList())
            val reviews: List<Review> = reviewsResult.getOrDefault(emptyList())

            // 4. Compute booking statistics
            val pendingCount = bookings.count { it.status == "pending" }
            val confirmedCount = bookings.count { it.status == "confirmed" }
            val inProgressCount = bookings.count { it.status == "in_progress" }
            val completedCount = bookings.count { it.status == "completed" }
            val totalBookingCount = bookings.size

            // 5. Compute revenue (only completed bookings count toward realized revenue)
            val totalRevenue = bookings.filter { it.status == "completed" }.sumOf { it.totalAmount }

            // Compute today's revenue (completed with timestamp within today)
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayEnd = todayStart + (24 * 60 * 60 * 1000L)

            val todayRevenue = bookings
                .filter { it.status == "completed" && it.updatedAt in todayStart until todayEnd }
                .sumOf { it.totalAmount }

            // 6. Compute ratings and reviews
            val reviewCount = reviews.size
            val averageRating = if (reviews.isNotEmpty()) {
                val avg = reviews.map { it.rating }.average()
                Math.round(avg * 10.0) / 10.0
            } else {
                garage.rating
            }

            // 7. Recent records
            val recentBookings = bookings
                .sortedWith(compareByDescending<Booking> { it.createdAt }.thenByDescending { it.scheduledDate })
                .take(6)
            val recentReviews = reviews.take(5)

            Result.success(
                GarageDashboardStats(
                    garage = garage,
                    pendingBookingCount = pendingCount,
                    confirmedBookingCount = confirmedCount,
                    inProgressBookingCount = inProgressCount,
                    completedBookingCount = completedCount,
                    totalBookingCount = totalBookingCount,
                    totalRevenue = totalRevenue,
                    todayRevenue = todayRevenue,
                    averageRating = averageRating,
                    reviewCount = reviewCount,
                    recentBookings = recentBookings,
                    listedServices = services,
                    serviceCount = services.size,
                    recentReviews = recentReviews
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
