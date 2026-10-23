package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.Review
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * ReviewRepository handles all Firestore operations for the "reviews" collection.
 *
 * Security note: Firestore rules already enforce that only the authenticated customer
 * whose UID matches request.resource.data.customerId may create a review.
 * Client-side eligibility checks here provide UX feedback before the network call.
 */
class ReviewRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val reviewsCollection = firestore.collection("reviews")

    /** Persist a new review document. Generates a new document ID if review.id is blank. */
    suspend fun addReview(review: Review): Result<Unit> {
        return try {
            val documentId = if (review.id.isEmpty()) reviewsCollection.document().id else review.id
            val newReview = review.copy(id = documentId)
            reviewsCollection.document(documentId).set(newReview).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Fetch all reviews for a specific garage, ordered newest-first. */
    suspend fun getReviewsByGarageId(garageId: String): Result<List<Review>> {
        return try {
            val snapshot = reviewsCollection.whereEqualTo("garageId", garageId).get().await()
            val reviews = snapshot.documents
                .mapNotNull { it.toObject(Review::class.java) }
                .sortedByDescending { it.createdAt }
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Fetch all reviews written by a specific customer. */
    suspend fun getReviewsByCustomerId(customerId: String): Result<List<Review>> {
        return try {
            val snapshot = reviewsCollection.whereEqualTo("customerId", customerId).get().await()
            val reviews = snapshot.documents
                .mapNotNull { it.toObject(Review::class.java) }
                .sortedByDescending { it.createdAt }
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check whether a review already exists for a specific (customerId, bookingId) pair.
     * Returns true if a duplicate exists, false if the customer may submit a new review.
     */
    suspend fun checkReviewExistsForBooking(customerId: String, bookingId: String): Result<Boolean> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("bookingId", bookingId)
                .limit(1)
                .get()
                .await()
            Result.success(snapshot.documents.isNotEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculate the average rating and review count for a garage.
     * Returns Pair(averageRating, reviewCount).
     */
    suspend fun getGarageRatingStats(garageId: String): Result<Pair<Double, Int>> {
        return try {
            val snapshot = reviewsCollection.whereEqualTo("garageId", garageId).get().await()
            val reviews = snapshot.documents.mapNotNull { it.toObject(Review::class.java) }
            val count = reviews.size
            val average = if (count == 0) 0.0 else reviews.sumOf { it.rating.toDouble() } / count
            val rounded = Math.round(average * 10.0) / 10.0
            Result.success(Pair(rounded, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update the denormalised rating and reviewCount fields on a garage document
     * so the garage listing reflects current review data without extra queries.
     */
    suspend fun updateGarageRatingStats(garageId: String): Result<Unit> {
        return try {
            val statsResult = getGarageRatingStats(garageId)
            if (statsResult.isFailure) return Result.failure(statsResult.exceptionOrNull()!!)
            val (avg, count) = statsResult.getOrThrow()
            firestore.collection("garages").document(garageId)
                .update(mapOf("rating" to avg, "reviewCount" to count))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
