package com.example.mad_project_akshaysathaye_c049.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests verifying ReviewValidator rules:
 *  - Authentication & ownership checks
 *  - Rating bounds [1f, 5f]
 *  - Comment length constraints [10, 500] characters
 */
class ReviewValidatorTest {

    @Test
    fun validReview_returnsNullError() {
        val review = Review(
            bookingId = "booking_123",
            garageId = "garage_456",
            customerId = "user_789",
            customerName = "John Doe",
            rating = 5f,
            comment = "Exceptional car detailing service! My car looks brand new."
        )
        val error = ReviewValidator.validate(review)
        assertNull("Valid review should pass validation without error", error)
    }

    @Test
    fun missingCustomerId_returnsError() {
        val review = Review(
            bookingId = "booking_123",
            garageId = "garage_456",
            customerId = "",
            rating = 5f,
            comment = "Great detailing work done."
        )
        val error = ReviewValidator.validate(review)
        assertNotNull(error)
        assertEquals("User authentication required to submit a review.", error)
    }

    @Test
    fun missingBookingId_returnsError() {
        val review = Review(
            bookingId = "",
            garageId = "garage_456",
            customerId = "user_789",
            rating = 4f,
            comment = "Great detailing work done."
        )
        val error = ReviewValidator.validate(review)
        assertNotNull(error)
        assertEquals("A valid booking is required to submit a review.", error)
    }

    @Test
    fun missingGarageId_returnsError() {
        val review = Review(
            bookingId = "booking_123",
            garageId = "",
            customerId = "user_789",
            rating = 4f,
            comment = "Great detailing work done."
        )
        val error = ReviewValidator.validate(review)
        assertNotNull(error)
        assertEquals("Garage information is missing.", error)
    }

    @Test
    fun ratingBelowMinimum_returnsError() {
        val review = Review(
            bookingId = "booking_123",
            garageId = "garage_456",
            customerId = "user_789",
            rating = 0.5f,
            comment = "Poor service and long delay."
        )
        val error = ReviewValidator.validate(review)
        assertNotNull(error)
        assertEquals("Please select a star rating between 1 and 5.", error)
    }

    @Test
    fun ratingAboveMaximum_returnsError() {
        val review = Review(
            bookingId = "booking_123",
            garageId = "garage_456",
            customerId = "user_789",
            rating = 6f,
            comment = "Awesome experience!"
        )
        val error = ReviewValidator.validate(review)
        assertNotNull(error)
        assertEquals("Please select a star rating between 1 and 5.", error)
    }

    @Test
    fun commentTooShort_returnsError() {
        val review = Review(
            bookingId = "booking_123",
            garageId = "garage_456",
            customerId = "user_789",
            rating = 5f,
            comment = "Short"
        )
        val error = ReviewValidator.validate(review)
        assertNotNull(error)
        assertEquals("Your review must be at least 10 characters long.", error)
    }

    @Test
    fun commentTooLong_returnsError() {
        val longComment = "a".repeat(501)
        val review = Review(
            bookingId = "booking_123",
            garageId = "garage_456",
            customerId = "user_789",
            rating = 5f,
            comment = longComment
        )
        val error = ReviewValidator.validate(review)
        assertNotNull(error)
        assertEquals("Your review must not exceed 500 characters.", error)
    }
}
