package com.example.mad_project_akshaysathaye_c049

import com.example.mad_project_akshaysathaye_c049.data.mock.AndheriGaragesData
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageImageProvider
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageImageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit verification suite ensuring 100% garage image coverage,
 * correct legal classification, and robust fallback behavior.
 */
class GarageImageProviderUnitTest {

    @Test
    fun testAllGaragesInDatabaseHaveDedicatedImages() {
        val garages = AndheriGaragesData.getGarages()
        assertEquals("AndheriGaragesData should contain exactly 20 garages", 20, garages.size)

        for (garage in garages) {
            val resId = GarageImageProvider.getGarageImageRes(garage.id)
            assertNotEquals(
                "Garage ${garage.id} (${garage.name}) should have a dedicated image, not default",
                GarageImageProvider.DEFAULT_IMAGE_RES,
                resId
            )
            assertTrue("Drawable resource id must be non-zero", resId != 0)

            val meta = GarageImageProvider.getMetadata(garage.id)
            assertEquals("Metadata garageId must match", garage.id, meta.garageId)
            assertTrue("Filename must not be empty", meta.filename.isNotBlank())
            assertTrue("Filename must end with .webp", meta.filename.endsWith(".webp"))
            assertTrue("Source/Status description must be present", meta.sourceOrStatus.isNotBlank())
            assertTrue("Legal notice must be present", meta.legalNotice.isNotBlank())
        }
    }

    @Test
    fun testFallbackBehaviorForUnknownOrBlankGarageId() {
        val unknownRes = GarageImageProvider.getGarageImageRes("garage_unknown_999")
        assertEquals(GarageImageProvider.DEFAULT_IMAGE_RES, unknownRes)

        val nullRes = GarageImageProvider.getGarageImageRes(null)
        assertEquals(GarageImageProvider.DEFAULT_IMAGE_RES, nullRes)

        val blankRes = GarageImageProvider.getGarageImageRes("   ")
        assertEquals(GarageImageProvider.DEFAULT_IMAGE_RES, blankRes)

        val unknownMeta = GarageImageProvider.getMetadata("garage_unknown_999")
        assertEquals(GarageImageType.DEFAULT_FALLBACK, unknownMeta.imageType)
    }

    @Test
    fun testLegalCategorizationAccuracy() {
        val all = GarageImageProvider.allGarages
        assertEquals("Total registered garages must be 20", 20, all.size)

        val aiCount = all.count { it.imageType == GarageImageType.AI_GENERATED }
        val curatedCount = all.count { it.imageType == GarageImageType.CURATED_WORKSHOP }

        assertEquals("Exactly 13 garages must be categorized as AI-generated illustrations", 13, aiCount)
        assertEquals("Exactly 7 garages must be categorized as curated workshop photos", 7, curatedCount)
    }
}
