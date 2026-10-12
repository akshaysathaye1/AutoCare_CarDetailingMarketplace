package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BookingRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val bookingsCollection = firestore.collection("bookings")

    /**
     * Create a new booking with a unique Firestore document ID.
     * Returns the created booking ID on success.
     */
    suspend fun createBooking(booking: Booking): Result<String> {
        return try {
            val documentId = if (booking.id.isBlank()) bookingsCollection.document().id else booking.id
            val newBooking = booking.copy(
                id = documentId,
                createdAt = if (booking.createdAt == 0L) System.currentTimeMillis() else booking.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            bookingsCollection.document(documentId).set(newBooking).await()
            Result.success(documentId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a single booking by ID.
     */
    suspend fun getBookingById(bookingId: String): Result<Booking?> {
        return try {
            val document = bookingsCollection.document(bookingId).get().await()
            val booking = document.toObject(Booking::class.java)
            Result.success(booking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieve all bookings for a customer, sorted newest first.
     */
    suspend fun getBookingsByCustomerId(customerId: String): Result<List<Booking>> {
        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("customerId", customerId)
                .get()
                .await()
            val bookings = snapshot.documents
                .mapNotNull { it.toObject(Booking::class.java) }
                .sortedWith(compareByDescending<Booking> { it.scheduledDate }.thenByDescending { it.createdAt })
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieve all bookings for a garage, sorted newest first.
     */
    suspend fun getBookingsByGarageId(garageId: String): Result<List<Booking>> {
        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("garageId", garageId)
                .get()
                .await()
            val bookings = snapshot.documents
                .mapNotNull { it.toObject(Booking::class.java) }
                .sortedWith(compareByDescending<Booking> { it.scheduledDate }.thenByDescending { it.createdAt })
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update booking status (e.g., pending -> confirmed, in_progress, completed, cancelled).
     */
    suspend fun updateBookingStatus(
        bookingId: String,
        newStatus: String,
        notes: String = ""
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to System.currentTimeMillis()
            )
            if (notes.isNotBlank()) {
                updates["notes"] = notes
            }
            bookingsCollection.document(bookingId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancel a booking with an optional reason.
     */
    suspend fun cancelBooking(bookingId: String, cancellationReason: String = ""): Result<Unit> {
        return try {
            val updates = mapOf<String, Any>(
                "status" to "cancelled",
                "cancellationReason" to cancellationReason,
                "updatedAt" to System.currentTimeMillis()
            )
            bookingsCollection.document(bookingId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if a time slot for a garage is already booked on the specified date.
     * Returns true if a conflicting active appointment exists.
     */
    suspend fun checkSlotConflict(
        garageId: String,
        scheduledDate: Long,
        scheduledTime: String
    ): Result<Boolean> {
        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("garageId", garageId)
                .whereEqualTo("scheduledDate", scheduledDate)
                .whereEqualTo("scheduledTime", scheduledTime)
                .get()
                .await()
            val activeBookings = snapshot.documents
                .mapNotNull { it.toObject(Booking::class.java) }
                .filter { it.status != "cancelled" }
            Result.success(activeBookings.isNotEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
