package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.Service
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ServiceRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val servicesCollection = firestore.collection("services")

    suspend fun createService(service: Service): Result<Unit> {
        return try {
            val documentId = if (service.id.isEmpty()) servicesCollection.document().id else service.id
            val newService = service.copy(id = documentId)
            servicesCollection.document(documentId).set(newService).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServicesByGarageId(garageId: String): Result<List<Service>> {
        return try {
            val snapshot = servicesCollection.whereEqualTo("garageId", garageId).get().await()
            val services = snapshot.documents.mapNotNull { it.toObject(Service::class.java) }
            Result.success(services)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
