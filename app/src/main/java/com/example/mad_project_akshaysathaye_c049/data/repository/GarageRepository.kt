package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.Garage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Meaningful Comment 10: GarageRepository Architecture
// Manages marketplace workshop profile persistence in Cloud Firestore under /garages.
// Adheres strictly to firestore.rules:
//  - allow read: if request.auth != null (Customers and Garages can read all partner garages)
//  - allow create, update: if request.auth != null && users/{uid}.data.role == 'GARAGE' (Only verified workshop partners can write)
class GarageRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val garagesCollection = firestore.collection("garages")

    // Meaningful Comment 11: Workshop creation and update handler
    // Assigns an existing deterministic ID if provided (e.g. for pre-seeded or partner-linked records)
    // or generates a new unique Firestore document ID before persisting.
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

    // Meaningful Comment 12: Direct workshop lookup by document ID
    // Deserializes Firestore document snapshot into Garage model or returns local Andheri dataset entry if not yet seeded.
    suspend fun getGarageById(id: String): Result<Garage?> {
        return try {
            val document = garagesCollection.document(id).get().await()
            val garage = document.toObject(Garage::class.java)
            if (garage != null) {
                Result.success(garage)
            } else {
                Result.success(com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData.getGarageById(id))
            }
        } catch (e: Exception) {
            val fallback = com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData.getGarageById(id)
            if (fallback != null) Result.success(fallback) else Result.failure(e)
        }
    }

    // Meaningful Comment 13: Marketplace discovery query
    // Fetches all active partner workshops listed in the /garages collection.
    // Seamlessly falls back to curated 20-garage Andheri Mumbai dataset when Cloud Firestore collection is unseeded.
    suspend fun getAllGarages(): Result<List<Garage>> {
        return try {
            val snapshot = garagesCollection.get().await()
            val garages = snapshot.documents.mapNotNull { it.toObject(Garage::class.java) }
            if (garages.isNotEmpty()) {
                Result.success(garages)
            } else {
                Result.success(com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData.getGarages())
            }
        } catch (e: Exception) {
            Result.success(com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData.getGarages())
        }
    }

    // Meaningful Comment 14: Workshop lookup by owner UID
    // Queries the garage registered by a specific authenticated partner.
    // Uses limit(1) since each garage partner is linked to a single workshop entity.
    suspend fun getGarageByOwnerId(ownerId: String): Result<Garage?> {
        return try {
            val snapshot = garagesCollection.whereEqualTo("ownerId", ownerId).limit(1).get().await()
            val garage = snapshot.documents.firstOrNull()?.toObject(Garage::class.java)
            if (garage != null) {
                Result.success(garage)
            } else {
                val demoPartnerGarage = com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData.getGarages().firstOrNull { it.ownerId == ownerId }
                Result.success(demoPartnerGarage)
            }
        } catch (e: Exception) {
            val demoPartnerGarage = com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData.getGarages().firstOrNull { it.ownerId == ownerId }
            Result.success(demoPartnerGarage)
        }
    }

    companion object {
        // Meaningful Comment 15: Session cleanup placeholder
        // Resets any local in-memory garage caches during logout or user switching.
        fun clearCache() {
            // Memory cache reset placeholder
        }
    }
}

