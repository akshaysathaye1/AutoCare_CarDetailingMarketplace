package com.example.mad_project_akshaysathaye_c049.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying:
 * 1. Booking status transition progression (Pending -> Confirmed -> In Progress -> Completed)
 * 2. Invalid transitions (Terminal states, skipped stages)
 * 3. Customer cancellation eligibility rules
 * 4. Customer review eligibility rules
 */
class BookingLifecycleUnitTest {

    @Test
    fun validForwardTransitions_arePermitted() {
        // Pending can advance to Confirmed
        assertTrue(
            "Pending should be able to advance to Confirmed",
            BookingStatus.isValidTransition(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        )

        // Confirmed can advance to In Progress
        assertTrue(
            "Confirmed should be able to advance to In Progress",
            BookingStatus.isValidTransition(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS)
        )

        // In Progress can advance to Completed
        assertTrue(
            "In Progress should be able to advance to Completed",
            BookingStatus.isValidTransition(BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED)
        )
    }

    @Test
    fun cancellationsAndRejections_arePermittedFromActiveStates() {
        // Pending can be declined/cancelled
        assertTrue(
            "Pending booking can be cancelled",
            BookingStatus.isValidTransition(BookingStatus.PENDING, BookingStatus.CANCELLED)
        )

        // Confirmed booking can be cancelled
        assertTrue(
            "Confirmed booking can be cancelled",
            BookingStatus.isValidTransition(BookingStatus.CONFIRMED, BookingStatus.CANCELLED)
        )

        // In progress booking can be cancelled if necessary
        assertTrue(
            "In progress booking can be cancelled",
            BookingStatus.isValidTransition(BookingStatus.IN_PROGRESS, BookingStatus.CANCELLED)
        )
    }

    @Test
    fun invalidAndSkippedTransitions_areRejected() {
        // Cannot jump directly from Pending to Completed without service in progress
        assertFalse(
            "Pending directly to Completed must be rejected",
            BookingStatus.isValidTransition(BookingStatus.PENDING, BookingStatus.COMPLETED)
        )

        // Cannot jump from Pending directly to In Progress without confirmation
        assertFalse(
            "Pending directly to In Progress must be rejected",
            BookingStatus.isValidTransition(BookingStatus.PENDING, BookingStatus.IN_PROGRESS)
        )

        // Cannot jump from Confirmed directly to Completed
        assertFalse(
            "Confirmed directly to Completed must be rejected",
            BookingStatus.isValidTransition(BookingStatus.CONFIRMED, BookingStatus.COMPLETED)
        )

        // Unknown status
        assertFalse(
            "Unknown status must be rejected",
            BookingStatus.isValidTransition(BookingStatus.PENDING, "unknown_status")
        )
    }

    @Test
    fun terminalStates_cannotTransition() {
        // Completed booking is final
        assertFalse(
            "Completed booking cannot transition to Cancelled",
            BookingStatus.isValidTransition(BookingStatus.COMPLETED, BookingStatus.CANCELLED)
        )
        assertFalse(
            "Completed booking cannot transition back to Pending",
            BookingStatus.isValidTransition(BookingStatus.COMPLETED, BookingStatus.PENDING)
        )
        assertFalse(
            "Completed booking cannot transition back to In Progress",
            BookingStatus.isValidTransition(BookingStatus.COMPLETED, BookingStatus.IN_PROGRESS)
        )

        // Cancelled booking is final
        assertFalse(
            "Cancelled booking cannot transition to Confirmed",
            BookingStatus.isValidTransition(BookingStatus.CANCELLED, BookingStatus.CONFIRMED)
        )
        assertFalse(
            "Cancelled booking cannot transition to Completed",
            BookingStatus.isValidTransition(BookingStatus.CANCELLED, BookingStatus.COMPLETED)
        )
    }

    @Test
    fun identicalStatusTransition_isIdempotent() {
        assertTrue(
            "Same status transition is valid (no-op)",
            BookingStatus.isValidTransition(BookingStatus.PENDING, BookingStatus.PENDING)
        )
        assertTrue(
            "Same status transition is valid (no-op)",
            BookingStatus.isValidTransition(BookingStatus.CONFIRMED, BookingStatus.CONFIRMED)
        )
    }

    @Test
    fun customerCancellationEligibility_rules() {
        // Allowed only while pending or confirmed
        assertTrue(
            "Customer can cancel Pending booking",
            BookingStatus.isCustomerCancellationAllowed(BookingStatus.PENDING)
        )
        assertTrue(
            "Customer can cancel Confirmed booking",
            BookingStatus.isCustomerCancellationAllowed(BookingStatus.CONFIRMED)
        )

        // Disallowed once in progress, completed, or already cancelled
        assertFalse(
            "Customer cannot cancel In Progress booking",
            BookingStatus.isCustomerCancellationAllowed(BookingStatus.IN_PROGRESS)
        )
        assertFalse(
            "Customer cannot cancel Completed booking",
            BookingStatus.isCustomerCancellationAllowed(BookingStatus.COMPLETED)
        )
        assertFalse(
            "Customer cannot cancel already Cancelled booking",
            BookingStatus.isCustomerCancellationAllowed(BookingStatus.CANCELLED)
        )
    }

    @Test
    fun reviewEligibility_rules() {
        // Only completed bookings can be reviewed
        assertTrue(
            "Review allowed for Completed booking",
            BookingStatus.isReviewAllowed(BookingStatus.COMPLETED)
        )

        assertFalse(
            "Review disallowed for Pending booking",
            BookingStatus.isReviewAllowed(BookingStatus.PENDING)
        )
        assertFalse(
            "Review disallowed for Confirmed booking",
            BookingStatus.isReviewAllowed(BookingStatus.CONFIRMED)
        )
        assertFalse(
            "Review disallowed for In Progress booking",
            BookingStatus.isReviewAllowed(BookingStatus.IN_PROGRESS)
        )
        assertFalse(
            "Review disallowed for Cancelled booking",
            BookingStatus.isReviewAllowed(BookingStatus.CANCELLED)
        )
    }
}
