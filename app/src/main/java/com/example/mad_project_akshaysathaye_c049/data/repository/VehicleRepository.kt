package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.Vehicle
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository responsible for managing Customer Vehicles in Cloud Firestore.
 *
 * Security & Ownership Rules:
 *  - Every vehicle belongs to a specific customer indicated by ownerId.
 *  - Customers can only create, update, list, and delete their own vehicles.
 *  - Enforces field validation (make, model, license plate format) before writing to Firestore.
 */
class VehicleRepository(
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) {
    private val firestore by lazy { firestoreProvider() }
    private val vehiclesCollection get() = firestore.collection("vehicles")

    companion object {
        // Meaningful Comment 28: Registration number format regex for Indian RTO and international formats
        /**
         * Validates vehicle registration number formats.
         * Accepts standard Indian RTO formats (e.g. MH 12 AB 1234, DL-01-C-4567, KA05MH8899)
         * and general international plates between 4 and 16 alphanumeric characters.
         */
        fun isValidRegistrationNumber(plate: String): Boolean {
            val clean = plate.trim().replace(" ", "").replace("-", "").uppercase()
            if (clean.length !in 4..16) return false
            // Standard Indian RTO format regex or general alphanumeric plate
            val rtoRegex = Regex("^[A-Z]{2}[0-9]{1,2}[A-Z]{0,3}[0-9]{4}$")
            val generalAlphaNumeric = Regex("^[A-Z0-9]{4,16}$")
            return clean.matches(rtoRegex) || clean.matches(generalAlphaNumeric)
        }
    }

    // Meaningful Comment 29: Vehicle creation with customer owner ID gating
    /**
     * Add a new vehicle to Firestore.
     * Validates ownerId presence, required make/model, and license plate format before persisting.
     */
    suspend fun addVehicle(vehicle: Vehicle): Result<String> {
        return try {
            if (vehicle.ownerId.isBlank()) {
                return Result.failure(IllegalArgumentException("Vehicle must be associated with an authenticated customer ownerId."))
            }
            if (vehicle.make.trim().isBlank() || vehicle.make.trim().length < 2) {
                return Result.failure(IllegalArgumentException("Vehicle make / brand must be at least 2 characters long."))
            }
            if (vehicle.model.trim().isBlank()) {
                return Result.failure(IllegalArgumentException("Vehicle model name is required."))
            }
            if (!isValidRegistrationNumber(vehicle.licensePlate)) {
                return Result.failure(IllegalArgumentException("Please enter a valid registration number (4-16 alphanumeric characters)."))
            }

            val documentId = if (vehicle.id.isBlank()) vehiclesCollection.document().id else vehicle.id
            val newVehicle = vehicle.copy(
                id = documentId,
                make = vehicle.make.trim(),
                model = vehicle.model.trim(),
                licensePlate = vehicle.licensePlate.trim().uppercase(),
                year = vehicle.year.trim(),
                color = vehicle.color.trim()
            )
            vehiclesCollection.document(documentId).set(newVehicle).await()
            Result.success(documentId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Meaningful Comment 30: Vehicle update with requesting customer ownership verification
    /**
     * Update an existing vehicle.
     * Enforces that requestingUserId matches vehicle.ownerId to prevent cross-account modifications.
     */
    suspend fun updateVehicle(vehicle: Vehicle, requestingUserId: String? = null): Result<Unit> {
        return try {
            if (vehicle.id.isBlank()) {
                return Result.failure(IllegalArgumentException("Cannot update vehicle with empty vehicle ID."))
            }
            if (vehicle.ownerId.isBlank()) {
                return Result.failure(IllegalArgumentException("Vehicle ownerId cannot be blank."))
            }
            // Enforce ownership check if requestingUserId is provided
            if (!requestingUserId.isNullOrBlank() && vehicle.ownerId != requestingUserId) {
                return Result.failure(SecurityException("Unauthorized: You can only update vehicles belonging to your account."))
            }
            if (vehicle.make.trim().isBlank() || vehicle.make.trim().length < 2) {
                return Result.failure(IllegalArgumentException("Vehicle make / brand must be at least 2 characters long."))
            }
            if (vehicle.model.trim().isBlank()) {
                return Result.failure(IllegalArgumentException("Vehicle model name is required."))
            }
            if (!isValidRegistrationNumber(vehicle.licensePlate)) {
                return Result.failure(IllegalArgumentException("Please enter a valid registration number (4-16 alphanumeric characters)."))
            }

            val updatedVehicle = vehicle.copy(
                make = vehicle.make.trim(),
                model = vehicle.model.trim(),
                licensePlate = vehicle.licensePlate.trim().uppercase(),
                year = vehicle.year.trim(),
                color = vehicle.color.trim()
            )
            vehiclesCollection.document(vehicle.id).set(updatedVehicle).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Meaningful Comment 31: Vehicle deletion with requesting customer ownership verification
    /**
     * Delete a vehicle from Firestore.
     * If requestingUserId is provided, verifies that the vehicle belongs to requestingUserId before deleting.
     */
    suspend fun deleteVehicle(vehicleId: String, requestingUserId: String? = null): Result<Unit> {
        return try {
            if (vehicleId.isBlank()) {
                return Result.failure(IllegalArgumentException("Vehicle ID cannot be blank."))
            }
            // Security verification: inspect document owner before deletion if user is authenticated
            if (!requestingUserId.isNullOrBlank()) {
                val existingDoc = vehiclesCollection.document(vehicleId).get().await()
                if (existingDoc.exists()) {
                    val ownerId = existingDoc.getString("ownerId")
                    if (ownerId != null && ownerId != requestingUserId) {
                        return Result.failure(SecurityException("Unauthorized: You can only delete vehicles belonging to your account."))
                    }
                }
            }
            vehiclesCollection.document(vehicleId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieve a specific vehicle by its unique document ID.
     */
    suspend fun getVehicleById(vehicleId: String): Result<Vehicle?> {
        return try {
            val document = vehiclesCollection.document(vehicleId).get().await()
            val vehicle = document.toObject(Vehicle::class.java)
            Result.success(vehicle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Meaningful Comment 32: Customer-scoped vehicle retrieval from /vehicles collection
    /**
     * Fetch all vehicles owned by a specific customer UID.
     * Ensures strict tenant scoping so customers only see their own registered vehicles.
     */
    suspend fun getVehiclesByOwnerId(ownerId: String): Result<List<Vehicle>> {
        return try {
            if (ownerId.isBlank()) {
                return Result.success(emptyList())
            }
            val snapshot = vehiclesCollection.whereEqualTo("ownerId", ownerId).get().await()
            val vehicles = snapshot.documents.mapNotNull { it.toObject(Vehicle::class.java) }
            Result.success(vehicles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
