package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.Vehicle
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class VehicleRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val vehiclesCollection = firestore.collection("vehicles")

    suspend fun addVehicle(vehicle: Vehicle): Result<Unit> {
        return try {
            val documentId = if (vehicle.id.isEmpty()) vehiclesCollection.document().id else vehicle.id
            val newVehicle = vehicle.copy(id = documentId)
            vehiclesCollection.document(documentId).set(newVehicle).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVehiclesByOwnerId(ownerId: String): Result<List<Vehicle>> {
        return try {
            val snapshot = vehiclesCollection.whereEqualTo("ownerId", ownerId).get().await()
            val vehicles = snapshot.documents.mapNotNull { it.toObject(Vehicle::class.java) }
            Result.success(vehicles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
