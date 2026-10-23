package com.example.mad_project_akshaysathaye_c049.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying aggregation logic for Customer and Garage Dashboards:
 *  - Exclusion of cancelled bookings from completed counts & revenue
 *  - Vehicle-to-service mapping accuracy
 *  - Average rating rounding to 1 decimal place
 *  - Accurate group-by frequency for service history
 */
class DashboardStatsUnitTest {

    @Test
    fun customerSpending_excludesCancelledBookings() {
        val bookings = listOf(
            Booking(id = "b1", status = "completed", totalAmount = 1500.0, serviceName = "Foam Wash"),
            Booking(id = "b2", status = "completed", totalAmount = 3500.0, serviceName = "Ceramic Coat"),
            Booking(id = "b3", status = "cancelled", totalAmount = 2000.0, serviceName = "Interior Spa"),
            Booking(id = "b4", status = "pending", totalAmount = 1000.0, serviceName = "Foam Wash")
        )

        val completed = bookings.filter { it.status == "completed" }
        val totalSpending = completed.sumOf { it.totalAmount }
        val completedCount = completed.size

        assertEquals(2, completedCount)
        assertEquals(5000.0, totalSpending, 0.001)
    }

    @Test
    fun vehicleServiceCounts_mappedAccurately() {
        val bookings = listOf(
            Booking(id = "b1", vehicleId = "veh_1", status = "completed"),
            Booking(id = "b2", vehicleId = "veh_1", status = "completed"),
            Booking(id = "b3", vehicleId = "veh_2", status = "completed"),
            Booking(id = "b4", vehicleId = "veh_2", status = "cancelled")
        )

        val completed = bookings.filter { it.status == "completed" && it.vehicleId.isNotBlank() }
        val vehicleCounts = completed.groupBy { it.vehicleId }.mapValues { it.value.size }

        assertEquals(2, vehicleCounts["veh_1"])
        assertEquals(1, vehicleCounts["veh_2"])
        assertEquals(null, vehicleCounts["veh_3"])
    }

    @Test
    fun serviceHistory_sortedByFrequencyDescending() {
        val bookings = listOf(
            Booking(id = "b1", serviceName = "Foam Wash", status = "completed"),
            Booking(id = "b2", serviceName = "Ceramic Coating", status = "completed"),
            Booking(id = "b3", serviceName = "Foam Wash", status = "completed"),
            Booking(id = "b4", serviceName = "Foam Wash", status = "completed"),
            Booking(id = "b5", serviceName = "Interior Detailing", status = "completed")
        )

        val completed = bookings.filter { it.status == "completed" }
        val history = completed
            .groupBy { it.serviceName }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }
            .associate { it.key to it.value }

        assertEquals(3, history["Foam Wash"])
        assertEquals(1, history["Ceramic Coating"])
        assertEquals("Foam Wash", history.keys.first())
    }

    @Test
    fun garageAverageRating_roundedToSingleDecimal() {
        val reviews = listOf(
            Review(rating = 5f),
            Review(rating = 4f),
            Review(rating = 4f)
        )
        // average = 4.3333333333... -> 4.3
        val avg = Math.round(reviews.map { it.rating }.average() * 10.0) / 10.0
        assertEquals(4.3, avg, 0.001)
    }

    @Test
    fun garageBookingStatusBreakdown_computedAccurately() {
        val bookings = listOf(
            Booking(id = "b1", status = "pending"),
            Booking(id = "b2", status = "confirmed"),
            Booking(id = "b3", status = "in_progress"),
            Booking(id = "b4", status = "completed", totalAmount = 2500.0),
            Booking(id = "b5", status = "completed", totalAmount = 1500.0),
            Booking(id = "b6", status = "cancelled")
        )

        val pending = bookings.count { it.status == "pending" }
        val confirmed = bookings.count { it.status == "confirmed" }
        val inProgress = bookings.count { it.status == "in_progress" }
        val completed = bookings.count { it.status == "completed" }
        val revenue = bookings.filter { it.status == "completed" }.sumOf { it.totalAmount }

        assertEquals(1, pending)
        assertEquals(1, confirmed)
        assertEquals(1, inProgress)
        assertEquals(2, completed)
        assertEquals(6, bookings.size)
        assertEquals(4000.0, revenue, 0.001)
    }
}
