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
            if (services.isNotEmpty()) {
                Result.success(services)
            } else {
                Result.success(com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData.getServicesForGarage(garageId))
            }
        } catch (e: Exception) {
            Result.success(com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData.getServicesForGarage(garageId))
        }
    }

    suspend fun getServiceById(serviceId: String): Result<Service?> {
        return try {
            val document = servicesCollection.document(serviceId).get().await()
            val service = document.toObject(Service::class.java)
            if (service != null) {
                Result.success(service)
            } else {
                Result.success(com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData.getServiceById(serviceId))
            }
        } catch (e: Exception) {
            val fallback = com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData.getServiceById(serviceId)
            if (fallback != null) Result.success(fallback) else Result.failure(e)
        }
    }

    suspend fun updateService(service: Service): Result<Unit> {
        return try {
            val documentId = if (service.id.isEmpty()) servicesCollection.document().id else service.id
            val updated = service.copy(id = documentId)
            servicesCollection.document(documentId).set(updated).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteService(serviceId: String): Result<Unit> {
        return try {
            servicesCollection.document(serviceId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
