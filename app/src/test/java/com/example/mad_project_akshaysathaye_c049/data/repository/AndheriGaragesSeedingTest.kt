package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndheriGaragesSeedingTest {

    @Test
    fun testTotalGaragesCountIsTwenty() {
        val garages = AndheriGaragesData.getGarages()
        assertEquals(20, garages.size)
    }

    @Test
    fun testAllGaragesHaveUniqueIds() {
        val garages = AndheriGaragesData.getGarages()
        val uniqueIds = garages.map { it.id }.toSet()
        assertEquals("Every garage must have a unique ID to prevent duplicates", garages.size, uniqueIds.size)
    }

    @Test
    fun testVerifiedVersusDemoGaragesCount() {
        val garages = AndheriGaragesData.getGarages()
        val verifiedCount = garages.count { it.isVerified && !it.isDemoData }
        val demoCount = garages.count { !it.isVerified && it.isDemoData }

        assertEquals(12, verifiedCount)
        assertEquals(8, demoCount)
        assertEquals(20, verifiedCount + demoCount)
    }

    @Test
    fun testAllGaragesLocatedInAndheriMumbaiLocalities() {
        val validLocalities = setOf(
            "Andheri East",
            "Andheri West",
            "Marol",
            "MIDC",
            "JB Nagar",
            "Saki Naka",
            "Chakala",
            "Versova"
        )
        val garages = AndheriGaragesData.getGarages()
        garages.forEach { garage ->
            assertTrue(
                "Garage ${garage.name} has invalid locality ${garage.locality}",
                validLocalities.contains(garage.locality)
            )
            assertEquals("Mumbai", garage.city)
        }
    }

    @Test
    fun testServicesArePopulatedAndPricedInInr() {
        val services = AndheriGaragesData.getAllServices()
        assertEquals(80, services.size) // 20 garages * 4 services each

        services.forEach { service ->
            assertNotNull(service.id)
            assertNotNull(service.garageId)
            assertTrue("Service price must be positive INR", service.price > 0)
            assertTrue("Service duration must be positive", service.durationMinutes > 0)
            assertTrue("Service category must be present", service.category.isNotBlank())
        }
    }

    @Test
    fun testGarageLookupAndServiceHandoff() {
        val garages = AndheriGaragesData.getGarages()
        garages.forEach { garage ->
            val foundGarage = AndheriGaragesData.getGarageById(garage.id)
            assertNotNull("Garage lookup by ID should succeed", foundGarage)
            assertEquals(garage.name, foundGarage?.name)

            val services = AndheriGaragesData.getServicesForGarage(garage.id)
            assertTrue("Each garage should have associated services", services.isNotEmpty())
            
            // Verify service selection
            val firstService = services.first()
            val retrievedService = AndheriGaragesData.getServiceById(firstService.id)
            assertNotNull("Service lookup by ID should succeed", retrievedService)
            assertEquals(firstService.id, retrievedService?.id)
            assertEquals(garage.id, retrievedService?.garageId)
        }
    }

    @Test
    fun testSafeRepeatableSeedWithoutDuplicates() {
        val run1 = AndheriGaragesData.getGarages()
        val run2 = AndheriGaragesData.getGarages()

        assertEquals(run1.size, run2.size)
        val map1 = run1.associateBy { it.id }
        val map2 = run2.associateBy { it.id }

        assertEquals(map1.keys, map2.keys)
    }
}
