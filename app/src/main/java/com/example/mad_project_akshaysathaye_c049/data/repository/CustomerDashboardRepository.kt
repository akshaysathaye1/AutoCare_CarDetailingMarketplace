package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.CustomerDashboardStats

/**
 * CustomerDashboardRepository aggregates data from BookingRepository and
 * VehicleRepository to produce a single CustomerDashboardStats snapshot.
 *
 * Design rationale: dashboard aggregation is isolated here so that the UI
 * composable remains a thin observer, and so this logic can be unit-tested
 * independently of the Compose lifecycle.
 */
class CustomerDashboardRepository(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val vehicleRepository: VehicleRepository = VehicleRepository()
) {
    /**
     * Load all data required for the Customer Dashboard in a single call.
     *
     * Rules:
     *  - Completed service count: bookings with status == "completed" only.
     *  - Total spending: sum of totalAmount for completed bookings only.
     *  - Cancelled bookings are excluded from both counts and spending.
     *  - Upcoming: status in {pending, confirmed, in_progress}.
     *  - Recent bookings: the 5 most-recently-created entries of any status.
     */
    suspend fun getDashboardStats(userId: String): Result<CustomerDashboardStats> {
        return try {
            val bookingsResult = bookingRepository.getBookingsByCustomerId(userId)
            val vehiclesResult = vehicleRepository.getVehiclesByOwnerId(userId)

            if (bookingsResult.isFailure) return Result.failure(bookingsResult.exceptionOrNull()!!)
            if (vehiclesResult.isFailure) return Result.failure(vehiclesResult.exceptionOrNull()!!)

            val allBookings = bookingsResult.getOrDefault(emptyList())
            val vehicles = vehiclesResult.getOrDefault(emptyList())

            val completedBookings = allBookings.filter { it.status == "completed" }
            val upcomingBookings = allBookings.filter {
                it.status in listOf("pending", "confirmed", "in_progress")
            }.sortedBy { it.scheduledDate }

            val completedServiceCount = completedBookings.size
            // Exclude cancelled; only count completed spending
            val totalSpending = completedBookings.sumOf { it.totalAmount }

            // Most recent 5 bookings by createdAt
            val recentBookings = allBookings
                .sortedByDescending { it.createdAt }
                .take(5)

            Result.success(
                CustomerDashboardStats(
                    completedServiceCount = completedServiceCount,
                    totalSpending = totalSpending,
                    upcomingBookingCount = upcomingBookings.size,
                    registeredVehicleCount = vehicles.size,
                    recentBookings = recentBookings,
                    upcomingBookings = upcomingBookings,
                    vehicles = vehicles
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
