package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.Review
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ReviewRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val reviewsCollection = firestore.collection("reviews")

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

    suspend fun getReviewsByGarageId(garageId: String): Result<List<Review>> {
        return try {
            val snapshot = reviewsCollection.whereEqualTo("garageId", garageId).get().await()
            val reviews = snapshot.documents.mapNotNull { it.toObject(Review::class.java) }
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
