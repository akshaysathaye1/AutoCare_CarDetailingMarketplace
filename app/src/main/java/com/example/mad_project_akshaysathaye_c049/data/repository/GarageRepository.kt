package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.Garage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GarageRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val garagesCollection = firestore.collection("garages")

    suspend fun createGarage(garage: Garage): Result<Unit> {
        return try {
            val documentId = if (garage.id.isEmpty()) garagesCollection.document().id else garage.id
            val newGarage = garage.copy(id = documentId)
            garagesCollection.document(documentId).set(newGarage).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGarageById(id: String): Result<Garage?> {
        return try {
            val document = garagesCollection.document(id).get().await()
            Result.success(document.toObject(Garage::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllGarages(): Result<List<Garage>> {
        return try {
            val snapshot = garagesCollection.get().await()
            val garages = snapshot.documents.mapNotNull { it.toObject(Garage::class.java) }
            Result.success(garages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
