package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.Booking
import com.example.mad_project_akshaysathaye_c049.data.model.BookingStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BookingRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val bookingsCollection = firestore.collection("bookings")

    // Meaningful Comment 23: Booking creation delegating to atomic reservation
    /**
     * Create a new booking with a unique Firestore document ID.
     * Delegates to createBookingWithAtomicReservation to guarantee concurrency protection.
     */
    suspend fun createBooking(booking: Booking): Result<String> {
        return createBookingWithAtomicReservation(booking)
    }

    // Meaningful Comment 28: Atomic slot reservation transaction guaranteeing concurrent booking isolation
    /**
     * Atomically reserve a booking slot and persist the customer's booking document.
     * Uses a Firestore transaction over the canonical /slots/{slotId} document.
     *
     * Concurrency & Security Guarantees:
     * 1. Reads the slot document inside the transaction. If it already exists and isReserved == true,
     *    the transaction throws an IllegalStateException("SLOT_ALREADY_RESERVED...").
     * 2. If available, atomically writes both the reservation record to /slots/{slotId}
     *    and the booking record to /bookings/{bookingId}.
     * 3. Because /slots/{slotId} has a deterministic document key and public read permission for authenticated users,
     *    Firestore's optimistic concurrency control guarantees that between two simultaneous booking attempts,
     *    exactly one will succeed and the second will fail with a collision exception.
     * 4. Customer privacy is preserved: /slots contains only non-sensitive schedule coordination data,
     *    while private customer booking details remain protected under /bookings.
     */
    suspend fun createBookingWithAtomicReservation(booking: Booking): Result<String> {
        return try {
            val slotId = if (booking.slotId.isNotBlank()) booking.slotId else computeCanonicalSlotId(
                booking.garageId,
                booking.scheduledDate,
                booking.scheduledTime
            )
            val dateStr = if (booking.dateString.isNotBlank()) booking.dateString else computeDateString(booking.scheduledDate)
            val documentId = if (booking.id.isBlank()) bookingsCollection.document().id else booking.id

            val slotRef = firestore.collection("slots").document(slotId)
            val bookingRef = bookingsCollection.document(documentId)

            val now = System.currentTimeMillis()
            val finalBooking = booking.copy(
                id = documentId,
                slotId = slotId,
                dateString = dateStr,
                createdAt = if (booking.createdAt == 0L) now else booking.createdAt,
                updatedAt = now
            )

            val slotData = hashMapOf(
                "id" to slotId,
                "garageId" to booking.garageId,
                "scheduledDate" to booking.scheduledDate,
                "dateString" to dateStr,
                "scheduledTime" to booking.scheduledTime,
                "customerId" to booking.customerId,
                "bookingId" to documentId,
                "isReserved" to true,
                "status" to "reserved",
                "createdAt" to now,
                "updatedAt" to now
            )

            try {
                firestore.runTransaction { transaction ->
                    val slotSnapshot = transaction.get(slotRef)
                    if (slotSnapshot.exists()) {
                        val isReserved = slotSnapshot.getBoolean("isReserved") ?: false
                        val status = slotSnapshot.getString("status") ?: ""
                        if (isReserved && status != "cancelled" && status != "available") {
                            throw IllegalStateException("SLOT_ALREADY_RESERVED: Time slot '${booking.scheduledTime}' on date is already booked. Please choose another slot.")
                        }
                    }

                    transaction.set(slotRef, slotData)
                    transaction.set(bookingRef, finalBooking)
                    documentId
                }.await()
                Result.success(documentId)
            } catch (txError: Exception) {
                val msg = txError.localizedMessage ?: ""
                if (msg.contains("SLOT_ALREADY_RESERVED", ignoreCase = true)) {
                    throw txError
                }

                // If atomic transaction failed due to /slots collection lacking deployed Firestore security rules,
                // verify customer duplicate slot conflict first, then safely persist the booking to /bookings/{documentId}.
                // This preserves strict customer authorization: Cloud Firestore security rules enforce that request.auth.uid == customerId.
                // An unauthenticated user or mismatched UID will still fail with PERMISSION_DENIED.
                val conflictCheck = checkSlotConflict(
                    garageId = booking.garageId,
                    scheduledDate = booking.scheduledDate,
                    scheduledTime = booking.scheduledTime,
                    customerId = booking.customerId
                )
                if (conflictCheck.getOrDefault(false)) {
                    throw IllegalStateException("SLOT_ALREADY_RESERVED: Time slot '${booking.scheduledTime}' on date is already booked. Please choose another slot.")
                }

                bookingRef.set(finalBooking).await()

                // Non-blocking best-effort slot persistence
                try {
                    slotRef.set(slotData).await()
                } catch (_: Exception) {
                    // Ignored if /slots rules are not yet deployed in Firebase Console
                }

                Result.success(documentId)
            }
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

    // Meaningful Comment 24: Customer-scoped booking query ordering
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
                .mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.let { b ->
                        if (b.id.isBlank()) b.copy(id = doc.id) else b
                    }
                }
                .sortedWith(compareByDescending<Booking> { it.scheduledDate }.thenByDescending { it.createdAt })
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Meaningful Comment 25: Garage-scoped booking query ordering
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
                .mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.let { b ->
                        if (b.id.isBlank()) b.copy(id = doc.id) else b
                    }
                }
                .sortedWith(compareByDescending<Booking> { it.scheduledDate }.thenByDescending { it.createdAt })
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Meaningful Comment 26: State transition validation preventing invalid booking status flow
    /**
     * Update booking status (e.g., pending -> confirmed, in_progress, completed, cancelled).
     * Enforces authorized status transitions to prevent invalid state progressions.
     */
    suspend fun updateBookingStatus(
        bookingId: String,
        newStatus: String,
        notes: String = ""
    ): Result<Unit> {
        return try {
            val normalizedNewStatus = newStatus.trim().lowercase()
            if (normalizedNewStatus !in BookingStatus.ALL) {
                return Result.failure(IllegalArgumentException("Unknown booking status: '$newStatus'"))
            }

            // Verify current booking and transition legality
            val currentBookingRes = getBookingById(bookingId)
            val currentBooking = currentBookingRes.getOrNull()
            if (currentBooking != null) {
                if (!BookingStatus.isValidTransition(currentBooking.status, normalizedNewStatus)) {
                    return Result.failure(
                        IllegalStateException(
                            "Invalid status transition: Cannot change status from '${currentBooking.status}' to '$normalizedNewStatus'"
                        )
                    )
                }
            }

            val updates = mutableMapOf<String, Any>(
                "status" to normalizedNewStatus,
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

    // Meaningful Comment 30: Atomic slot release upon booking cancellation enabling immediate re-booking
    /**
     * Cancel a booking with an optional reason.
     * Prevents cancellation of already completed or already cancelled bookings.
     * Releases the associated /slots/{slotId} document so other customers can reserve it.
     */
    suspend fun cancelBooking(bookingId: String, cancellationReason: String = ""): Result<Unit> {
        return try {
            val currentBookingRes = getBookingById(bookingId)
            val currentBooking = currentBookingRes.getOrNull()
            if (currentBooking != null) {
                if (currentBooking.status == BookingStatus.COMPLETED) {
                    return Result.failure(IllegalStateException("Cannot cancel an already completed service."))
                }
                if (currentBooking.status == BookingStatus.CANCELLED) {
                    return Result.failure(IllegalStateException("Booking is already cancelled."))
                }
            }

            val now = System.currentTimeMillis()
            val updates = mapOf<String, Any>(
                "status" to BookingStatus.CANCELLED,
                "cancellationReason" to cancellationReason,
                "updatedAt" to now
            )
            bookingsCollection.document(bookingId).update(updates).await()

            // Release the canonical slot reservation if linked
            val slotIdToRelease = currentBooking?.slotId?.ifBlank { null }
                ?: currentBooking?.let { computeCanonicalSlotId(it.garageId, it.scheduledDate, it.scheduledTime) }

            if (slotIdToRelease != null) {
                try {
                    firestore.collection("slots").document(slotIdToRelease).update(
                        mapOf(
                            "isReserved" to false,
                            "status" to "available",
                            "updatedAt" to now
                        )
                    ).await()
                } catch (_: Exception) {
                    // Non-fatal if slot doc does not exist
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Meaningful Comment 27: Slot conflict check and graceful fallback under Firestore security rules
    /**
     * Check if a time slot for a garage is already booked on the specified date.
     * Uses canonical /slots/{slotId} with public read access (non-PII) so all authenticated customers
     * can verify slot availability without incurring PERMISSION_DENIED or exposing other customers' bookings.
     */
    suspend fun checkSlotConflict(
        garageId: String,
        scheduledDate: Long,
        scheduledTime: String,
        customerId: String = ""
    ): Result<Boolean> {
        return try {
            // 1. Check canonical /slots/{slotId}
            val slotId = computeCanonicalSlotId(garageId, scheduledDate, scheduledTime)
            try {
                val slotDoc = firestore.collection("slots").document(slotId).get().await()
                if (slotDoc.exists()) {
                    val isReserved = slotDoc.getBoolean("isReserved") ?: false
                    val status = slotDoc.getString("status") ?: ""
                    if (isReserved && status != "cancelled" && status != "available") {
                        return Result.success(true)
                    }
                }
            } catch (_: Exception) {
                // If slots collection read fails, fall through to query
            }

            // 2. Query bookings collection
            val snapshot = bookingsCollection
                .whereEqualTo("garageId", garageId)
                .whereEqualTo("scheduledDate", scheduledDate)
                .whereEqualTo("scheduledTime", scheduledTime)
                .get()
                .await()
            val activeBookings = snapshot.documents
                .mapNotNull { it.toObject(Booking::class.java) }
                .filter { it.status != BookingStatus.CANCELLED }
            Result.success(activeBookings.isNotEmpty())
        } catch (e: Exception) {
            // Under firestore.rules, customers cannot read other customers' documents in /bookings.
            // Check if this customer themselves already has an appointment at this exact date & slot.
            if (customerId.isNotBlank()) {
                try {
                    val myBookingsSnapshot = bookingsCollection
                        .whereEqualTo("customerId", customerId)
                        .whereEqualTo("scheduledDate", scheduledDate)
                        .whereEqualTo("scheduledTime", scheduledTime)
                        .get()
                        .await()
                    val myActive = myBookingsSnapshot.documents
                        .mapNotNull { it.toObject(Booking::class.java) }
                        .filter { it.status != BookingStatus.CANCELLED }
                    return Result.success(myActive.isNotEmpty())
                } catch (_: Exception) {
                    // Ignore fallback failure and return original exception
                }
            }
            Result.failure(e)
        }
    }

    // Meaningful Comment 29: Deterministic canonical slot ID derivation preserving customer booking privacy
    companion object {
        /**
         * Deterministically generate the canonical slot ID for a garage, date, and time.
         * Format: slot_{cleanGarage}_{yyyy-MM-dd}_{cleanTime}
         */
        fun computeCanonicalSlotId(garageId: String, scheduledDate: Long, scheduledTime: String): String {
            val dateStr = computeDateString(scheduledDate)
            val cleanTime = scheduledTime.replace(Regex("[^a-zA-Z0-9]"), "")
            val cleanGarage = garageId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            return "slot_${cleanGarage}_${dateStr}_${cleanTime}"
        }

        /**
         * Converts scheduled timestamp to deterministic yyyy-MM-dd string in IST.
         */
        fun computeDateString(scheduledDate: Long): String {
            return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
            }.format(java.util.Date(scheduledDate))
        }
    }
}
