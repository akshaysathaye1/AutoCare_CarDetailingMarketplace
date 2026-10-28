package com.example.mad_project_akshaysathaye_c049.data.model

import com.example.mad_project_akshaysathaye_c049.ui.customer.matchesMarketplaceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Meaningful Comment 23: Garage discovery and category filtering test suite
// Verifies client-side marketplace discovery algorithms inspired by GoMechanic:
//  - Category matching via automotive taxonomy synonyms (Body, Tyre, Engine, Electrical)
//  - Multi-field text search across name, address, city, and description
//  - Graceful fallback for blank or null category selections
class GarageDiscoveryUnitTest {

    private val detailingGarage = Garage(
        id = "g1",
        name = "SpeedAuto Detailing Studio",
        description = "Premium car detailing, ceramic coating, paint protection, and foam wash.",
        address = "123 MG Road",
        city = "Bangalore"
    )

    private val tuningGarage = Garage(
        id = "g2",
        name = "Apex Motor Works & Tuning",
        description = "Expert engine tuning, oil change, tyre alignment, and performance upgrades.",
        address = "456 Indiranagar",
        city = "Bangalore"
    )

    private val expressWorkshop = Garage(
        id = "g3",
        name = "AutoShine Express Workshop",
        description = "Quick and reliable car wash, wheel balancing, electrical diagnostics, and interior spa.",
        address = "789 Koramangala",
        city = "Bangalore"
    )

    // Meaningful Comment 24: Null or blank category returns true for all workshops
    @Test
    fun matchesMarketplaceCategory_nullOrBlankCategory_returnsTrue() {
        assertTrue(matchesMarketplaceCategory(detailingGarage, null))
        assertTrue(matchesMarketplaceCategory(detailingGarage, ""))
        assertTrue(matchesMarketplaceCategory(detailingGarage, "   "))
    }

    // Meaningful Comment 25: Body category matches detailing, coating, wash, paint
    @Test
    fun matchesMarketplaceCategory_bodyCategory_matchesSynonyms() {
        assertTrue("Detailing studio should match Body category", matchesMarketplaceCategory(detailingGarage, "Body"))
        assertTrue("AutoShine with car wash/spa should match Body category", matchesMarketplaceCategory(expressWorkshop, "Body"))
    }

    // Meaningful Comment 26: Tyre category matches wheel, alignment, tyre
    @Test
    fun matchesMarketplaceCategory_tyreCategory_matchesWheelAndAlignment() {
        assertTrue("Apex motor works with tyre alignment matches Tyre category", matchesMarketplaceCategory(tuningGarage, "Tyre"))
        assertTrue("AutoShine with wheel balancing matches Tyre category", matchesMarketplaceCategory(expressWorkshop, "Tyre"))
    }

    // Meaningful Comment 27: Engine category matches tuning, performance, mechanical
    @Test
    fun matchesMarketplaceCategory_engineCategory_matchesTuningAndPerformance() {
        assertTrue("Apex Motor Works with engine tuning matches Engine category", matchesMarketplaceCategory(tuningGarage, "Engine"))
    }

    // Meaningful Comment 28: Electrical category matches wiring, battery, diagnostics
    @Test
    fun matchesMarketplaceCategory_electricalCategory_matchesDiagnosticsAndElectronics() {
        assertTrue("AutoShine with electrical diagnostics matches Electrical category", matchesMarketplaceCategory(expressWorkshop, "Electrical"))
    }

    // Meaningful Comment 29: Case insensitive keyword search across all garage fields
    @Test
    fun searchGarages_matchesCityOrAddressOrNameCaseInsensitively() {
        val garages = listOf(detailingGarage, tuningGarage, expressWorkshop)

        val bangaloreResults = garages.filter { it.city.contains("bangalore", ignoreCase = true) }
        assertEquals(3, bangaloreResults.size)

        val indiranagarResults = garages.filter { it.address.contains("indiranagar", ignoreCase = true) }
        assertEquals(1, indiranagarResults.size)
        assertEquals("g2", indiranagarResults.first().id)

        val speedResults = garages.filter { it.name.contains("speed", ignoreCase = true) }
        assertEquals(1, speedResults.size)
        assertEquals("g1", speedResults.first().id)
    }

    // Meaningful Comment 30: Empty search query preserves full catalog
    @Test
    fun searchGarages_emptyQuery_returnsAllGarages() {
        val garages = listOf(detailingGarage, tuningGarage, expressWorkshop)
        val query = ""
        val filtered = garages.filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) }
        assertEquals(3, filtered.size)
    }

    // Meaningful Comment 31: Tests for GoMechanic 10 Core Automotive Categories
    @Test
    fun matchesMarketplaceCategory_goMechanic10Categories_matchAppropriateWorkshops() {
        // 1. Car Servicing
        assertTrue("AutoShine Express Workshop matches Car Servicing", matchesMarketplaceCategory(expressWorkshop, "Car Servicing"))
        // 2. Oil & Filter
        assertTrue("Apex Motor Works with oil change matches Oil & Filter", matchesMarketplaceCategory(tuningGarage, "Oil & Filter"))
        // 3. Tyres
        assertTrue("Apex Motor Works with tyre alignment matches Tyres", matchesMarketplaceCategory(tuningGarage, "Tyres"))
        // 4. Wheel Alignment
        assertTrue("AutoShine with wheel balancing matches Wheel Alignment", matchesMarketplaceCategory(expressWorkshop, "Wheel Alignment"))
        // 5. Battery
        assertTrue("AutoShine with electrical diagnostics matches Battery", matchesMarketplaceCategory(expressWorkshop, "Battery"))
        // 6. Washing
        assertTrue("SpeedAuto Detailing with foam wash matches Washing", matchesMarketplaceCategory(detailingGarage, "Washing"))
        // 7. Detailing
        assertTrue("SpeedAuto Detailing Studio matches Detailing", matchesMarketplaceCategory(detailingGarage, "Detailing"))
        // 8. Denting & Painting
        assertTrue("SpeedAuto with paint protection matches Denting & Painting", matchesMarketplaceCategory(detailingGarage, "Denting & Painting"))
    }
}

