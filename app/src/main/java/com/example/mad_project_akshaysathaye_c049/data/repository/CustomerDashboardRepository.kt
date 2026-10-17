package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.CustomerDashboardStats

/**
 * CustomerDashboardRepository aggregates data from BookingRepository and
 * VehicleRepository to produce a single CustomerDashboardStats snapshot.
 */
class CustomerDashboardRepository(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val vehicleRepository: VehicleRepository = VehicleRepository()
) {
    /**
     * Load all data required for the Customer Dashboard.
     * Cancelled bookings are excluded from service counts and spending totals.
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
            val totalSpending = completedBookings.sumOf { it.totalAmount }
            val recentBookings = allBookings.sortedByDescending { it.createdAt }.take(5)

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

    /**
     * Returns service history as serviceName -> count, from completed bookings.
     * Sorted by frequency descending for most-used service display.
     */
    suspend fun getServiceHistory(userId: String): Result<Map<String, Int>> {
        return try {
            val result = bookingRepository.getBookingsByCustomerId(userId)
            if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
            val completed = result.getOrDefault(emptyList()).filter { it.status == "completed" }
            val grouped = completed
                .groupBy { it.serviceName.ifBlank { "Unknown Service" } }
                .mapValues { it.value.size }
                .entries.sortedByDescending { it.value }
                .associate { it.key to it.value }
            Result.success(grouped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns spending breakdown garageName -> totalSpent for completed bookings only.
     * Sorted by spending descending for most-visited garage indicator.
     */
    suspend fun getSpendingByGarage(userId: String): Result<Map<String, Double>> {
        return try {
            val result = bookingRepository.getBookingsByCustomerId(userId)
            if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
            val completed = result.getOrDefault(emptyList()).filter { it.status == "completed" }
            val grouped = completed
                .groupBy { it.garageName.ifBlank { "Unknown Garage" } }
                .mapValues { entry -> entry.value.sumOf { it.totalAmount } }
                .entries.sortedByDescending { it.value }
                .associate { it.key to it.value }
            Result.success(grouped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
