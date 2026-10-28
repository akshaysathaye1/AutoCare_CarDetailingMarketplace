package com.example.mad_project_akshaysathaye_c049.data.model

data class Booking(
    val id: String = "",
    val customerId: String = "",
    val garageId: String = "",
    val serviceId: String = "",
    val vehicleId: String = "",
    val slotId: String = "",
    val dateString: String = "",
    val status: String = "pending", // pending, confirmed, in_progress, completed, cancelled
    val scheduledDate: Long = 0L,
    val scheduledTime: String = "",
    val totalAmount: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val garageName: String = "",
    val garageAddress: String = "",
    val serviceName: String = "",
    val vehicleDetails: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val cancellationReason: String = "",
    val paymentMethod: String = "pay_at_garage", // pay_at_garage, online_test
    val paymentStatus: String = "pending",       // pending, completed, failed, pending_verification
    val paymentTransactionId: String = ""
)

/**
 * Supported payment methods across the marketplace.
 */
object PaymentMethod {
    const val PAY_AT_GARAGE = "pay_at_garage"
    const val ONLINE_TEST = "online_test"
}

/**
 * Status tracking for booking payments.
 */
object PaymentStatus {
    const val PENDING = "pending"
    const val PENDING_VERIFICATION = "pending_verification"
    const val COMPLETED = "completed"
    const val FAILED = "failed"
}

/**
 * Standard statuses and lifecycle validation for Bookings.
 */
object BookingStatus {
    const val PENDING = "pending"
    const val CONFIRMED = "confirmed"
    const val IN_PROGRESS = "in_progress"
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"

    val ALL = setOf(PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED)

    /**
     * Determines whether transitioning from currentStatus to newStatus is valid.
     * Authorized flow:
     * pending -> confirmed or cancelled
     * confirmed -> in_progress or cancelled
     * in_progress -> completed or cancelled
     * completed -> [terminal, cannot be altered]
     * cancelled -> [terminal, cannot be altered]
     */
    fun isValidTransition(currentStatus: String, newStatus: String): Boolean {
        val curr = currentStatus.trim().lowercase()
        val next = newStatus.trim().lowercase()

        if (curr == next) return true
        if (next !in ALL) return false

        return when (curr) {
            PENDING -> next == CONFIRMED || next == CANCELLED
            CONFIRMED -> next == IN_PROGRESS || next == CANCELLED
            IN_PROGRESS -> next == COMPLETED || next == CANCELLED
            COMPLETED -> false // Terminal state
            CANCELLED -> false // Terminal state
            else -> false
        }
    }

    /**
     * Checks if a customer is allowed to cancel an appointment.
     * Customers may only cancel pending or confirmed appointments.
     * In-progress or completed appointments cannot be cancelled by customers.
     */
    fun isCustomerCancellationAllowed(status: String): Boolean {
        val s = status.trim().lowercase()
        return s == PENDING || s == CONFIRMED
    }

    /**
     * Reviews can only be submitted for completed bookings.
     */
    fun isReviewAllowed(status: String): Boolean {
        return status.trim().lowercase() == COMPLETED
    }
}

