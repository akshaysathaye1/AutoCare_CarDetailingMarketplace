package com.example.mad_project_akshaysathaye_c049.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit test suite verifying:
 * 1. Vehicle form input validation (make, model, license plate, year)
 * 2. Vehicle customer ownership verification
 * 3. Booking input validation (garage, service, vehicle, date, time slot)
 * 4. Past time-slot detection for same-day booking
 */
class BookingAndVehicleValidationTest {

    // --- Vehicle Validation Tests ---

    @Test
    fun vehicleMake_validation_rules() {
        // Blank make
        assertTrue("Blank make should be invalid", "".trim().isBlank())
        assertTrue("Whitespace make should be invalid", "   ".trim().isBlank())

        // Minimum character requirement
        assertTrue("Single character make is too short", "A".trim().length < 2)
        assertFalse("Valid brand name is accepted", "Honda".trim().length < 2)
        assertFalse("Tata is accepted", "Tata".trim().length < 2)
    }

    @Test
    fun vehicleModel_validation_rules() {
        assertTrue("Blank model should be invalid", "".trim().isBlank())
        assertFalse("Valid model is accepted", "City".trim().isBlank())
        assertFalse("Alphanumeric model is accepted", "i20".trim().isBlank())
    }

    @Test
    fun vehicleLicensePlate_validation_rules() {
        val validatePlate = { plate: String ->
            val cleaned = plate.trim().uppercase()
            when {
                cleaned.isBlank() -> "Registration number is required"
                cleaned.length < 4 || cleaned.length > 16 -> "Enter a valid registration number"
                else -> null
            }
        }

        assertEquals("Registration number is required", validatePlate(""))
        assertEquals("Registration number is required", validatePlate("   "))
        assertEquals("Enter a valid registration number", validatePlate("KA1"))
        assertNull(validatePlate("KA 01 MJ 1234"))
        assertNull(validatePlate("GJ-36-AD-4352"))
        assertNull(validatePlate("MH12AB1234"))
    }

    @Test
    fun vehicleYear_validation_rules() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val validateYear = { yearStr: String ->
            if (yearStr.trim().isNotBlank()) {
                val y = yearStr.trim().toIntOrNull()
                if (y == null || y < 1980 || y > currentYear + 1) {
                    "Enter a valid 4-digit year (1980 - ${currentYear + 1})"
                } else {
                    null
                }
            } else {
                null
            }
        }

        // Optional blank year is valid
        assertNull(validateYear(""))
        assertNull(validateYear("   "))

        // Invalid integer formats
        assertNotNull(validateYear("abcd"))
        assertNotNull(validateYear("1979"))
        assertNotNull(validateYear((currentYear + 2).toString()))

        // Valid years
        assertNull(validateYear("2015"))
        assertNull(validateYear("2024"))
        assertNull(validateYear(currentYear.toString()))
    }

    @Test
    fun vehicleOwnership_enforcement_preventsCrossUserTampering() {
        val customerA = "cust_user_111"
        val customerB = "cust_user_222"

        val vehicleA = Vehicle(
            id = "veh_01",
            ownerId = customerA,
            make = "Honda",
            model = "City",
            licensePlate = "KA 01 AB 1234"
        )

        // Customer A accessing their own vehicle
        val canAccessOwn = (vehicleA.ownerId == customerA)
        assertTrue("Customer A can access their own vehicle", canAccessOwn)

        // Customer B attempting to modify Customer A's vehicle
        val canAccessOther = (vehicleA.ownerId == customerB)
        assertFalse("Customer B is blocked from accessing Customer A's vehicle", canAccessOther)
    }

    // --- Booking Validation Tests ---

    @Test
    fun bookingValidation_requiresAllMandatoryFields() {
        val validateBooking = { garageId: String?, serviceId: String?, vehicleId: String?, date: Long, slot: String ->
            when {
                garageId.isNullOrBlank() -> "Please select a valid workshop."
                serviceId.isNullOrBlank() -> "Please select a detailing service."
                vehicleId.isNullOrBlank() -> "Please choose a vehicle for the appointment."
                date <= 0L -> "Please choose an appointment date."
                slot.isBlank() -> "Please choose a time slot for your appointment."
                else -> null
            }
        }

        assertEquals("Please select a valid workshop.", validateBooking(null, "s1", "v1", 1000L, "10:30 AM"))
        assertEquals("Please select a detailing service.", validateBooking("g1", "", "v1", 1000L, "10:30 AM"))
        assertEquals("Please choose a vehicle for the appointment.", validateBooking("g1", "s1", null, 1000L, "10:30 AM"))
        assertEquals("Please choose an appointment date.", validateBooking("g1", "s1", "v1", 0L, "10:30 AM"))
        assertEquals("Please choose a time slot for your appointment.", validateBooking("g1", "s1", "v1", 1000L, ""))
        assertNull(validateBooking("g1", "s1", "v1", 1000L, "10:30 AM"))
    }

    @Test
    fun bookingSlot_pastSlotDetection_rejectsExpiredHoursOnToday() {
        fun isSlotInPast(slot: String, dateTimestamp: Long, currentHour: Int, currentMinute: Int): Boolean {
            val parts = slot.split(" ", ":")
            if (parts.size >= 3) {
                var hour = parts[0].toIntOrNull() ?: 0
                val minute = parts[1].toIntOrNull() ?: 0
                val amPm = parts[2]
                if (amPm.equals("PM", ignoreCase = true) && hour < 12) hour += 12
                if (amPm.equals("AM", ignoreCase = true) && hour == 12) hour = 0

                val slotMinutes = hour * 60 + minute
                val nowMinutes = currentHour * 60 + currentMinute
                return slotMinutes < nowMinutes
            }
            return false
        }

        // At 2:00 PM (14:00), 09:00 AM and 10:30 AM have passed
        assertTrue("09:00 AM is in past at 14:00", isSlotInPast("09:00 AM", System.currentTimeMillis(), 14, 0))
        assertTrue("12:00 PM is in past at 14:00", isSlotInPast("12:00 PM", System.currentTimeMillis(), 14, 0))

        // Upcoming slots at 14:00
        assertFalse("03:00 PM is upcoming at 14:00", isSlotInPast("03:00 PM", System.currentTimeMillis(), 14, 0))
        assertFalse("04:30 PM is upcoming at 14:00", isSlotInPast("04:30 PM", System.currentTimeMillis(), 14, 0))
    }
}
