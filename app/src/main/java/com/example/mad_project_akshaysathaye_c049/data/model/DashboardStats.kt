package com.example.mad_project_akshaysathaye_c049.data.model

/**
 * Aggregated statistics for the Customer Dashboard screen.
 * All numeric fields are computed from real Firestore records; no values are hardcoded.
 */
data class CustomerDashboardStats(
    val completedServiceCount: Int = 0,
    val totalSpending: Double = 0.0,
    val upcomingBookingCount: Int = 0,
    val registeredVehicleCount: Int = 0,
    val recentBookings: List<Booking> = emptyList(),
    val upcomingBookings: List<Booking> = emptyList(),
    val vehicles: List<Vehicle> = emptyList()
)

/**
 * Aggregated statistics for the Garage Dashboard screen.
 * All numeric fields are computed from real Firestore records; no values are hardcoded.
 */
data class GarageDashboardStats(
    val garage: Garage? = null,
    val pendingBookingCount: Int = 0,
    val confirmedBookingCount: Int = 0,
    val completedBookingCount: Int = 0,
    val totalBookingCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val recentBookings: List<Booking> = emptyList(),
    val serviceCount: Int = 0
)
