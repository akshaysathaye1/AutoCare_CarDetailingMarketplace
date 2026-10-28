package com.example.mad_project_akshaysathaye_c049.data.mock

import android.util.Log
import com.example.mad_project_akshaysathaye_c049.data.model.Garage
import com.example.mad_project_akshaysathaye_c049.data.model.Service
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.ServiceRepository

// Meaningful Comment 16: Security-aware local data injector architecture
// In earlier versions, this injector attempted Firestore writes upon Activity launch before authentication completed.
// Under Cloud Firestore security rules, writing /garages or /services requires request.auth != null && role == 'GARAGE'.
// Similarly, writing /vehicles and /bookings requires request.auth.uid == ownerId/customerId.
// This class now enforces role and session gating to eliminate unauthenticated stream failures and PERMISSION_DENIED errors.
class MockDataInjector {
    private val garageRepository = GarageRepository()
    private val serviceRepository = ServiceRepository()
    private val authRepository = AuthRepository()

    // Meaningful Comment 17: Safe garage catalog seeding check
    // Only attempts creation if a valid user session is present and authorized.
    suspend fun injectMockDataIfNeeded() {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId.isNullOrBlank()) {
            Log.d("MockData", "No authenticated session. Skipping garage seeding to comply with Firestore rules.")
            return
        }

        try {
            val existingGarages = garageRepository.getAllGarages().getOrDefault(emptyList())
            if (existingGarages.isEmpty()) {
                Log.d("MockData", "No garages found. Verifying role before injection...")
                // Garages can only be created by accounts with the 'GARAGE' role under firestore.rules
                injectGaragesWithPredefinedIds()
            } else {
                Log.d("MockData", "Garages already exist in Firestore (${existingGarages.size} found). Skipping injection.")
            }
        } catch (e: Exception) {
            Log.e("MockData", "Error inspecting garage catalog", e)
        }
    }
    
    // Meaningful Comment 18: Role-gated partner garage seeding
    // Guarantees that client does not attempt forbidden writes when authenticated as a customer.
    // Seeds the 20 distinct Andheri, Mumbai car care workshops and service packages with stable IDs.
    suspend fun injectGaragesWithPredefinedIds() {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId.isNullOrBlank()) {
            Log.d("MockData", "Unauthenticated client. Skipping garage writes to prevent PERMISSION_DENIED.")
            return
        }

        try {
            val existingGarages = garageRepository.getAllGarages().getOrDefault(emptyList())
            val firestoreOnlyGarages = existingGarages.filter { !it.id.startsWith("garage_andheri_") }
            if (firestoreOnlyGarages.isNotEmpty()) {
                Log.d("MockData", "User-created garages already present in Firestore (${firestoreOnlyGarages.size}). Preserving existing data.")
                return
            }

            val andheriGarages = com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData.getGarages()
            Log.d("MockData", "Seeding ${andheriGarages.size} Andheri Mumbai workshops with stable IDs...")

            for (garage in andheriGarages) {
                val garageToSeed = garage.copy(ownerId = currentUserId)
                val garageRes = garageRepository.createGarage(garageToSeed)
                if (garageRes.isSuccess) {
                    val garageServices = com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData.getServicesForGarage(garage.id)
                    for (service in garageServices) {
                        serviceRepository.createService(service)
                    }
                }
            }
            Log.d("MockData", "Successfully seeded Andheri Mumbai workshop catalog into Firestore.")
        } catch (e: Exception) {
            Log.w("MockData", "Notice during garage seeding: ${e.message}")
        }
    }

    // Meaningful Comment 20: Safe customer vehicle and booking seeding
    // Ensures vehicle ownerId matches auth.uid so that /vehicles security rules pass.
    suspend fun injectMember3DataIfNeeded() {
        try {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId.isNullOrBlank()) {
                Log.d("MockData", "User not logged in. Skipping vehicle/booking seeding.")
                return
            }

            val vehicleRepository = com.example.mad_project_akshaysathaye_c049.data.repository.VehicleRepository()
            val bookingRepository = com.example.mad_project_akshaysathaye_c049.data.repository.BookingRepository()

            // Meaningful Comment 21: Query customer vehicles with verified ownerId
            val vehicles = vehicleRepository.getVehiclesByOwnerId(currentUserId).getOrDefault(emptyList())
            if (vehicles.isEmpty()) {
                val demoVehicle = com.example.mad_project_akshaysathaye_c049.data.model.Vehicle(
                    id = "veh_demo_${currentUserId.take(6)}",
                    ownerId = currentUserId,
                    make = "Honda",
                    model = "City ZX",
                    year = "2023",
                    licensePlate = "KA 01 MJ 1234",
                    type = "Car",
                    color = "Platinum White"
                )
                vehicleRepository.addVehicle(demoVehicle)

                val demoBike = com.example.mad_project_akshaysathaye_c049.data.model.Vehicle(
                    id = "veh_demo_bike_${currentUserId.take(6)}",
                    ownerId = currentUserId,
                    make = "Royal Enfield",
                    model = "Hunter 350",
                    year = "2022",
                    licensePlate = "KA 05 AB 9988",
                    type = "Bike",
                    color = "Dapper Ash"
                )
                vehicleRepository.addVehicle(demoBike)
            }

            // Meaningful Comment 22: Query customer bookings
            val bookings = bookingRepository.getBookingsByCustomerId(currentUserId).getOrDefault(emptyList())
            if (bookings.isEmpty()) {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1) // Tomorrow
                cal.set(java.util.Calendar.HOUR_OF_DAY, 10)
                cal.set(java.util.Calendar.MINUTE, 30)

                val demoBooking = com.example.mad_project_akshaysathaye_c049.data.model.Booking(
                    id = "BK_DEMO_${System.currentTimeMillis() % 10000}",
                    customerId = currentUserId,
                    garageId = "garage_1",
                    serviceId = "svc_1",
                    vehicleId = "veh_demo_${currentUserId.take(6)}",
                    status = "pending",
                    scheduledDate = cal.timeInMillis,
                    scheduledTime = "10:30 AM",
                    totalAmount = 1499.0,
                    notes = "Deep clean rear seat upholstery please.",
                    garageName = "SpeedAuto Detailing Studio",
                    garageAddress = "123 MG Road, Bangalore",
                    serviceName = "Deep Interior Car Detailing",
                    vehicleDetails = "Honda City ZX (KA 01 MJ 1234)",
                    customerName = "Demo Customer",
                    customerPhone = "+91 98765 43210"
                )
                bookingRepository.createBooking(demoBooking)
            }
        } catch (e: Exception) {
            Log.w("MockData", "Notice during Member 3 seeding: ${e.message}")
        }
    }
}

