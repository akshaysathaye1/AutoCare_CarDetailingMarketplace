package com.example.mad_project_akshaysathaye_c049.data.mock

import android.util.Log
import com.example.mad_project_akshaysathaye_c049.data.model.Garage
import com.example.mad_project_akshaysathaye_c049.data.model.Service
import com.example.mad_project_akshaysathaye_c049.data.repository.GarageRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.ServiceRepository

class MockDataInjector {
    private val garageRepository = GarageRepository()
    private val serviceRepository = ServiceRepository()

    suspend fun injectMockDataIfNeeded() {
        try {
            val existingGarages = garageRepository.getAllGarages().getOrDefault(emptyList())
            if (existingGarages.isEmpty()) {
                Log.d("MockData", "No garages found. Injecting mock data...")
                
                val mockGarages = listOf(
                    Garage(
                        name = "SpeedAuto Detailing Studio",
                        description = "Premium car detailing and ceramic coating.",
                        address = "123 MG Road",
                        city = "Bangalore",
                        contactNumber = "9876543210",
                        rating = 4.8,
                        reviewCount = 120
                    ),
                    Garage(
                        name = "Apex Motor Works & Tuning",
                        description = "Expert engine tuning and performance upgrades.",
                        address = "456 Indiranagar",
                        city = "Bangalore",
                        contactNumber = "8765432109",
                        rating = 4.9,
                        reviewCount = 85
                    ),
                    Garage(
                        name = "AutoShine Express Workshop",
                        description = "Quick and reliable car wash and interior spa.",
                        address = "789 Koramangala",
                        city = "Bangalore",
                        contactNumber = "7654321098",
                        rating = 4.6,
                        reviewCount = 200
                    )
                )

                for (garage in mockGarages) {
                    val result = garageRepository.createGarage(garage)
                    // If we successfully created it, we could also fetch the ID to inject services,
                    // but since createGarage generates an ID inside if empty, we might need a modified approach.
                    // Actually, let's just create garage with a predefined ID for mock data.
                }
            } else {
                Log.d("MockData", "Garages already exist. Skipping injection.")
            }
        } catch (e: Exception) {
            Log.e("MockData", "Error injecting mock data", e)
        }
    }
    
    suspend fun injectGaragesWithPredefinedIds() {
        val existingGarages = garageRepository.getAllGarages().getOrDefault(emptyList())
        if (existingGarages.isEmpty()) {
            val mockGarages = listOf(
                Garage(
                    id = "garage_1",
                    name = "SpeedAuto Detailing Studio",
                    description = "Premium car detailing and ceramic coating.",
                    address = "123 MG Road",
                    city = "Bangalore",
                    contactNumber = "9876543210",
                    rating = 4.8,
                    reviewCount = 120
                ),
                Garage(
                    id = "garage_2",
                    name = "Apex Motor Works & Tuning",
                    description = "Expert engine tuning and performance upgrades.",
                    address = "456 Indiranagar",
                    city = "Bangalore",
                    contactNumber = "8765432109",
                    rating = 4.9,
                    reviewCount = 85
                ),
                Garage(
                    id = "garage_3",
                    name = "AutoShine Express Workshop",
                    description = "Quick and reliable car wash and interior spa.",
                    address = "789 Koramangala",
                    city = "Bangalore",
                    contactNumber = "7654321098",
                    rating = 4.6,
                    reviewCount = 200
                )
            )

            for (garage in mockGarages) {
                garageRepository.createGarage(garage)
                
                // Add some services for each garage
                val mockServices = listOf(
                    Service(garageId = garage.id, name = "Foam Wash", price = 499.0, category = "Washing"),
                    Service(garageId = garage.id, name = "Ceramic Coating", price = 6999.0, category = "Detailing"),
                    Service(garageId = garage.id, name = "Interior Spa", price = 1299.0, category = "Cleaning")
                )
                for (service in mockServices) {
                    serviceRepository.createService(service)
                }
            }
        }
    }

    suspend fun injectMember3DataIfNeeded() {
        try {
            val vehicleRepository = com.example.mad_project_akshaysathaye_c049.data.repository.VehicleRepository()
            val bookingRepository = com.example.mad_project_akshaysathaye_c049.data.repository.BookingRepository()
            val authRepository = com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository()
            val customerId = authRepository.getEffectiveUserId("CUSTOMER")

            val vehicles = vehicleRepository.getVehiclesByOwnerId(customerId).getOrDefault(emptyList())
            if (vehicles.isEmpty()) {
                val demoVehicle = com.example.mad_project_akshaysathaye_c049.data.model.Vehicle(
                    id = "veh_demo_1",
                    ownerId = customerId,
                    make = "Honda",
                    model = "City ZX",
                    year = "2023",
                    licensePlate = "KA 01 MJ 1234",
                    type = "Car",
                    color = "Platinum White"
                )
                vehicleRepository.addVehicle(demoVehicle)

                val demoBike = com.example.mad_project_akshaysathaye_c049.data.model.Vehicle(
                    id = "veh_demo_2",
                    ownerId = customerId,
                    make = "Royal Enfield",
                    model = "Hunter 350",
                    year = "2022",
                    licensePlate = "KA 05 AB 9988",
                    type = "Bike",
                    color = "Dapper Ash"
                )
                vehicleRepository.addVehicle(demoBike)
            }

            val bookings = bookingRepository.getBookingsByCustomerId(customerId).getOrDefault(emptyList())
            if (bookings.isEmpty()) {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1) // Tomorrow
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)

                val demoBooking = com.example.mad_project_akshaysathaye_c049.data.model.Booking(
                    id = "BK_DEMO_1029",
                    customerId = customerId,
                    garageId = "garage_1",
                    serviceId = "svc_1",
                    vehicleId = "veh_demo_1",
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
            android.util.Log.e("MockData", "Error injecting Member 3 mock data", e)
        }
    }
}
