package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.Booking
import com.example.mad_project_akshaysathaye_c049.data.model.BookingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Unit tests verifying:
 * 1. Deterministic canonical slot ID generation for concurrency isolation.
 * 2. Slot date formatting in Asia/Kolkata timezone.
 * 3. Secure review rating aggregation (1-decimal rounding, 0-baseline, invalid-rating filtering).
 * 4. Booking model slot association and cancellation boundary rules.
 */
class BookingReservationSecurityTest {

    private fun createTimestamp(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun computeCanonicalSlotId_isDeterministicForIdenticalSlot() {
        val dateTs = createTimestamp(2030, 8, 15)
        val slotId1 = BookingRepository.computeCanonicalSlotId("garage_apex", dateTs, "10:00 AM")
        val slotId2 = BookingRepository.computeCanonicalSlotId("garage_apex", dateTs, "10:00 AM")

        assertEquals("Canonical slot ID must be perfectly deterministic for identical inputs", slotId1, slotId2)
        assertTrue("Slot ID must contain sanitized garage identifier", slotId1.contains("garage_apex"))
        assertTrue("Slot ID must contain formatted date string", slotId1.contains("2030-08-15"))
        assertTrue("Slot ID must contain sanitized time", slotId1.contains("1000AM"))
    }

    @Test
    fun computeCanonicalSlotId_differentiatesDifferentTimes() {
        val dateTs = createTimestamp(2030, 8, 15)
        val slotMorning = BookingRepository.computeCanonicalSlotId("garage_apex", dateTs, "09:00 AM")
        val slotAfternoon = BookingRepository.computeCanonicalSlotId("garage_apex", dateTs, "02:00 PM")

        assertNotEquals("Different time slots must generate distinct slot document IDs", slotMorning, slotAfternoon)
    }

    @Test
    fun computeCanonicalSlotId_differentiatesDifferentDates() {
        val date1 = createTimestamp(2030, 8, 15)
        val date2 = createTimestamp(2030, 8, 16)
        val slotDay1 = BookingRepository.computeCanonicalSlotId("garage_apex", date1, "10:00 AM")
        val slotDay2 = BookingRepository.computeCanonicalSlotId("garage_apex", date2, "10:00 AM")

        assertNotEquals("Different calendar dates must generate distinct slot document IDs", slotDay1, slotDay2)
    }

    @Test
    fun computeCanonicalSlotId_differentiatesDifferentGarages() {
        val dateTs = createTimestamp(2030, 8, 15)
        val slotGarageA = BookingRepository.computeCanonicalSlotId("garage_alpha", dateTs, "10:00 AM")
        val slotGarageB = BookingRepository.computeCanonicalSlotId("garage_beta", dateTs, "10:00 AM")

        assertNotEquals("Different garages must generate distinct slot document IDs", slotGarageA, slotGarageB)
    }

    @Test
    fun computeCanonicalSlotId_sanitizesSpecialCharacters() {
        val dateTs = createTimestamp(2030, 8, 15)
        val rawGarageId = "garage#special!@123"
        val slotId = BookingRepository.computeCanonicalSlotId(rawGarageId, dateTs, "11:30 AM")

        assertFalse("Slot ID must not contain hash", slotId.contains("#"))
        assertFalse("Slot ID must not contain exclamation", slotId.contains("!"))
        assertFalse("Slot ID must not contain spaces or colons", slotId.contains(" ") || slotId.contains(":"))
        assertTrue("Slot ID must start with standard prefix slot_", slotId.startsWith("slot_"))
    }

    @Test
    fun computeDateString_formatsIstDateConsistently() {
        val dateTs = createTimestamp(2032, 12, 25)
        val dateStr = BookingRepository.computeDateString(dateTs)
        assertEquals("2032-12-25", dateStr)
    }

    @Test
    fun reviewRatingAggregate_calculatesAccuratelyWithZeroBaseline() {
        // Empty reviews list produces 0.0 rating and 0 count
        val emptyAggregate = ReviewRepository.calculateRatingAggregate(emptyList())
        assertEquals(0.0, emptyAggregate.first, 0.001)
        assertEquals(0, emptyAggregate.second)

        // Single review
        val singleAggregate = ReviewRepository.calculateRatingAggregate(listOf(5.0f))
        assertEquals(5.0, singleAggregate.first, 0.001)
        assertEquals(1, singleAggregate.second)

        // Multiple reviews: 5.0, 4.0, 5.0, 5.0 -> avg = 19/4 = 4.75 -> rounded to 4.8
        val multiAggregate = ReviewRepository.calculateRatingAggregate(listOf(5.0f, 4.0f, 5.0f, 5.0f))
        assertEquals(4.8, multiAggregate.first, 0.001)
        assertEquals(4, multiAggregate.second)

        // Filters out invalid ratings below 1.0 and above 5.0
        val withInvalid = ReviewRepository.calculateRatingAggregate(listOf(5.0f, 0.0f, 6.5f, 3.0f))
        assertEquals(4.0, withInvalid.first, 0.001)
        assertEquals(2, withInvalid.second)
    }

    @Test
    fun bookingModel_preservesSlotMetadata() {
        val booking = Booking(
            id = "test_booking_1",
            customerId = "cust_1",
            garageId = "garage_1",
            serviceId = "srv_1",
            vehicleId = "veh_1",
            slotId = "slot_garage_1_2030-08-15_1000AM",
            dateString = "2030-08-15",
            scheduledTime = "10:00 AM",
            status = BookingStatus.PENDING
        )

        assertEquals("slot_garage_1_2030-08-15_1000AM", booking.slotId)
        assertEquals("2030-08-15", booking.dateString)

        val updated = booking.copy(status = BookingStatus.CONFIRMED)
        assertEquals("slot_garage_1_2030-08-15_1000AM", updated.slotId)
        assertEquals(BookingStatus.CONFIRMED, updated.status)
    }

    @Test
    fun bookingCancellationRules_disallowTerminalStateTransitions() {
        // Completed bookings cannot be cancelled
        assertFalse(
            "Completed bookings cannot transition to cancelled",
            BookingStatus.isValidTransition(BookingStatus.COMPLETED, BookingStatus.CANCELLED)
        )

        // Cancelled bookings cannot transition back to pending or confirmed
        assertFalse(
            "Cancelled bookings cannot transition to pending",
            BookingStatus.isValidTransition(BookingStatus.CANCELLED, BookingStatus.PENDING)
        )
        assertFalse(
            "Cancelled bookings cannot transition to confirmed",
            BookingStatus.isValidTransition(BookingStatus.CANCELLED, BookingStatus.CONFIRMED)
        )
    }
}
