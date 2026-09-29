package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BookingRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val bookingsCollection = firestore.collection("bookings")

    suspend fun createBooking(booking: Booking): Result<Unit> {
        return try {
            val documentId = if (booking.id.isEmpty()) bookingsCollection.document().id else booking.id
            val newBooking = booking.copy(id = documentId)
            bookingsCollection.document(documentId).set(newBooking).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookingsByCustomerId(customerId: String): Result<List<Booking>> {
        return try {
            val snapshot = bookingsCollection.whereEqualTo("customerId", customerId).get().await()
            val bookings = snapshot.documents.mapNotNull { it.toObject(Booking::class.java) }
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookingsByGarageId(garageId: String): Result<List<Booking>> {
        return try {
            val snapshot = bookingsCollection.whereEqualTo("garageId", garageId).get().await()
            val bookings = snapshot.documents.mapNotNull { it.toObject(Booking::class.java) }
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
