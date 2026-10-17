package com.example.mad_project_akshaysathaye_c049.data.model

/**
 * Review data class representing a customer rating and comment for a completed booking.
 *
 * Firestore collection: "reviews"
 * Key constraints enforced by ReviewValidator:
 *  - rating must be in [1, 5]
 *  - comment must be non-blank and between 10–500 characters
 *  - bookingId, garageId, customerId must be non-empty
 */
data class Review(
    val id: String = "",
    val bookingId: String = "",
    val garageId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Validates a Review before it is persisted to Firestore.
 * Returns null on success or a human-readable error message on failure.
 */
object ReviewValidator {
    private const val MIN_RATING = 1f
    private const val MAX_RATING = 5f
    private const val MIN_COMMENT_LENGTH = 10
    private const val MAX_COMMENT_LENGTH = 500

    fun validate(review: Review): String? {
        if (review.customerId.isBlank()) return "User authentication required to submit a review."
        if (review.bookingId.isBlank()) return "A valid booking is required to submit a review."
        if (review.garageId.isBlank()) return "Garage information is missing."
        if (review.rating < MIN_RATING || review.rating > MAX_RATING) {
            return "Please select a star rating between 1 and 5."
        }
        if (review.comment.isBlank()) return "Please write a comment before submitting."
        if (review.comment.trim().length < MIN_COMMENT_LENGTH) {
            return "Your review must be at least $MIN_COMMENT_LENGTH characters long."
        }
        if (review.comment.trim().length > MAX_COMMENT_LENGTH) {
            return "Your review must not exceed $MAX_COMMENT_LENGTH characters."
        }
        return null // valid
    }
}
