package com.example.mad_project_akshaysathaye_c049.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mad_project_akshaysathaye_c049.R

/**
 * Categorization of garage imagery conforming to legal and transparency guidelines.
 */
enum class GarageImageType(val label: String) {
    AI_GENERATED("AI-Generated Illustration"),
    CURATED_WORKSHOP("Curated Workshop Photo"),
    DEFAULT_FALLBACK("AutoCare Marketplace Asset")
}

/**
 * Detailed metadata describing each garage asset, its origin, licensing, and file mapping.
 */
data class GarageImageMetadata(
    val garageId: String,
    val garageName: String,
    @get:DrawableRes val drawableRes: Int,
    val filename: String,
    val imageType: GarageImageType,
    val sourceOrStatus: String,
    val legalNotice: String
)

/**
 * Central provider and resolver for all garage imagery across AutoCare.
 * Guarantees zero empty/broken placeholders by mapping every known garage ID
 * to its local bundled asset, with an offline-ready fallback.
 */
object GarageImageProvider {

    @DrawableRes
    val DEFAULT_IMAGE_RES: Int = R.drawable.garage_default

    private val registry: Map<String, GarageImageMetadata> = mapOf(
        "garage_andheri_01" to GarageImageMetadata(
            garageId = "garage_andheri_01",
            garageName = "Sai Service",
            drawableRes = R.drawable.garage_andheri_01,
            filename = "garage_andheri_01.webp",
            imageType = GarageImageType.AI_GENERATED,
            sourceOrStatus = "AI-generated illustration depicting an authorized multi-bay hydraulic lift workshop on Western Express Highway",
            legalNotice = "Original synthesized illustrative asset - not an official photograph of the business"
        ),
        "garage_andheri_02" to GarageImageMetadata(
            garageId = "garage_andheri_02",
            garageName = "Shivam Autozone",
            drawableRes = R.drawable.garage_andheri_02,
            filename = "garage_andheri_02.webp",
            imageType = GarageImageType.AI_GENERATED,
            sourceOrStatus = "AI-generated illustration of a spacious MIDC industrial auto maintenance and express service facility",
            legalNotice = "Original synthesized illustrative asset - not an official photograph of the business"
        ),
        "garage_andheri_03" to GarageImageMetadata(
            garageId = "garage_andheri_03",
            garageName = "Auto Hangar",
            drawableRes = R.drawable.garage_andheri_03,
            filename = "garage_andheri_03.webp",
            imageType = GarageImageType.AI_GENERATED,
            sourceOrStatus = "AI-generated illustration of a luxury ceramic coating and detailing studio with hexagonal LED illumination",
            legalNotice = "Original synthesized illustrative asset - not an official photograph of the business"
        ),
        "garage_andheri_04" to GarageImageMetadata(
            garageId = "garage_andheri_04",
            garageName = "Car Clinic",
            drawableRes = R.drawable.garage_andheri_04,
            filename = "garage_andheri_04.webp",
            imageType = GarageImageType.AI_GENERATED,
            sourceOrStatus = "AI-generated illustration of computer engine diagnostics, ECU tuning, and multi-sensor inspection",
            legalNotice = "Original synthesized illustrative asset - not an official photograph of the business"
        ),
        "garage_andheri_05" to GarageImageMetadata(
            garageId = "garage_andheri_05",
            garageName = "Windshield Experts",
            drawableRes = R.drawable.garage_andheri_05,
            filename = "garage_andheri_05.webp",
            imageType = GarageImageType.AI_GENERATED,
            sourceOrStatus = "AI-generated illustration of automotive glass replacement, polyurethane sealant bonding, and windshield care",
            legalNotice = "Original synthesized illustrative asset - not an official photograph of the business"
        ),
        "garage_andheri_06" to GarageImageMetadata(
            garageId = "garage_andheri_06",
            garageName = "Fort Point Automotive",
            drawableRes = R.drawable.garage_andheri_06,
            filename = "garage_andheri_06.webp",
            imageType = GarageImageType.AI_GENERATED,
            sourceOrStatus = "AI-generated illustration of a multi-brand workshop with service bays, tire care, and cleaning facilities",
            legalNotice = "Original synthesized illustrative asset - not an official photograph of the business"
        ),
        "garage_andheri_07" to GarageImageMetadata(
            garageId = "garage_andheri_07",
            garageName = "JMD Auto Garage",
            drawableRes = R.drawable.garage_andheri_07,
            filename = "garage_andheri_07.webp",
            imageType = GarageImageType.AI_GENERATED,
            sourceOrStatus = "AI-generated illustration of suspension overhaul, tie rods, and computerized alignment ramps",
            legalNotice = "Original synthesized illustrative asset - not an official photograph of the business"
        ),
        "garage_andheri_08" to GarageImageMetadata(
            garageId = "garage_andheri_08",
            garageName = "Richard Garage",
            drawableRes = R.drawable.garage_andheri_08,
            filename = "garage_andheri_08.webp",
            imageType = GarageImageType.AI_GENERATED,
            sourceOrStatus = "AI-generated illustration of mechanical cylinder head, timing belt, and engine overhaul",
            legalNotice = "Original synthesized illustrative asset - not an official photograph of the business"
        ),
        "garage_andheri_09" to GarageImageMetadata(
            garageId = "garage_andheri_09",
            garageName = "TAJ Auto Garage",
            drawableRes = R.drawable.garage_andheri_09,
            filename = "garage_andheri_09.webp",
            imageType = GarageImageType.AI_GENERATED,
            sourceOrStatus = "AI-generated illustration of disc brake rotor surfacing, ceramic brake pads, and hydraulic calipers",
            legalNotice = "Original synthesized illustrative asset - not an official photograph of the business"
        ),
        "garage_andheri_10" to GarageImageMetadata(
            garageId = "garage_andheri_10",
            garageName = "Synergy Motors",
            drawableRes = R.drawable.garage_andheri_10,
            filename = "garage_andheri_10.webp",
            imageType = GarageImageType.AI_GENERATED,
            sourceOrStatus = "AI-generated illustration of synthetic engine oil change, filter replacement, and digital OBD-II scanning",
            legalNotice = "Original synthesized illustrative asset - not an official photograph of the business"
        ),
        "garage_andheri_11" to GarageImageMetadata(
            garageId = "garage_andheri_11",
            garageName = "A S Motor Works",
            drawableRes = R.drawable.garage_andheri_11,
            filename = "garage_andheri_11.webp",
            imageType = GarageImageType.AI_GENERATED,
            sourceOrStatus = "AI-generated illustration of an active automotive wash bay with high-pressure snow foam",
            legalNotice = "Original synthesized illustrative asset - not an official photograph of the business"
        ),
        "garage_andheri_12" to GarageImageMetadata(
            garageId = "garage_andheri_12",
            garageName = "Dev Motor Solution",
            drawableRes = R.drawable.garage_andheri_12,
            filename = "garage_andheri_12.webp",
            imageType = GarageImageType.AI_GENERATED,
            sourceOrStatus = "AI-generated illustration of coil spring suspension, strut assembly, and torque calibration",
            legalNotice = "Original synthesized illustrative asset - not an official photograph of the business"
        ),
        "garage_andheri_13" to GarageImageMetadata(
            garageId = "garage_andheri_13",
            garageName = "Allied Motor Garage",
            drawableRes = R.drawable.garage_andheri_13,
            filename = "garage_andheri_13.webp",
            imageType = GarageImageType.AI_GENERATED,
            sourceOrStatus = "AI-generated illustration of oil drain catch basins, undercarriage lubrication, and filter service",
            legalNotice = "Original synthesized illustrative asset - not an official photograph of the business"
        ),
        "garage_andheri_14" to GarageImageMetadata(
            garageId = "garage_andheri_14",
            garageName = "Cardekho Workshop",
            drawableRes = R.drawable.garage_andheri_14,
            filename = "garage_andheri_14.webp",
            imageType = GarageImageType.CURATED_WORKSHOP,
            sourceOrStatus = "Oyo State Computerized Vehicle Inspection Service Center (Wikimedia Commons)",
            legalNotice = "Creative Commons CC0 1.0 Universal Public Domain Dedication"
        ),
        "garage_andheri_15" to GarageImageMetadata(
            garageId = "garage_andheri_15",
            garageName = "Rampal Auto Garage",
            drawableRes = R.drawable.garage_andheri_15,
            filename = "garage_andheri_15.webp",
            imageType = GarageImageType.CURATED_WORKSHOP,
            sourceOrStatus = "Modern car wheel balancing machine tool at tire service by Shixart1985 (Wikimedia Commons)",
            legalNotice = "Creative Commons Attribution 2.0 Generic (CC BY 2.0)"
        ),
        "garage_andheri_16" to GarageImageMetadata(
            garageId = "garage_andheri_16",
            garageName = "Zenita Auto Works",
            drawableRes = R.drawable.garage_andheri_16,
            filename = "garage_andheri_16.webp",
            imageType = GarageImageType.CURATED_WORKSHOP,
            sourceOrStatus = "Car mechanic's workbench with automotive tools and components (Wikimedia Commons / Unsplash)",
            legalNotice = "Creative Commons CC0 / Free Unsplash License"
        ),
        "garage_andheri_17" to GarageImageMetadata(
            garageId = "garage_andheri_17",
            garageName = "Banai Auto Works",
            drawableRes = R.drawable.garage_andheri_17,
            filename = "garage_andheri_17.webp",
            imageType = GarageImageType.CURATED_WORKSHOP,
            sourceOrStatus = "Car restoration and paint bodywork workshop by Malte Luk (Wikimedia Commons / Unsplash)",
            legalNotice = "Creative Commons CC0 / Free Unsplash License"
        ),
        "garage_andheri_18" to GarageImageMetadata(
            garageId = "garage_andheri_18",
            garageName = "Jagman Motors",
            drawableRes = R.drawable.garage_andheri_18,
            filename = "garage_andheri_18.webp",
            imageType = GarageImageType.CURATED_WORKSHOP,
            sourceOrStatus = "Car repair shop with vehicle lifts and hydraulic platforms by Roman Boed (Wikimedia Commons)",
            legalNotice = "Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)"
        ),
        "garage_andheri_19" to GarageImageMetadata(
            garageId = "garage_andheri_19",
            garageName = "Car Solutions",
            drawableRes = R.drawable.garage_andheri_19,
            filename = "garage_andheri_19.webp",
            imageType = GarageImageType.CURATED_WORKSHOP,
            sourceOrStatus = "AZ Auto Detailing interior and exterior workshop (Wikimedia Commons)",
            legalNotice = "Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)"
        ),
        "garage_andheri_20" to GarageImageMetadata(
            garageId = "garage_andheri_20",
            garageName = "Karim Auto Centre",
            drawableRes = R.drawable.garage_andheri_20,
            filename = "garage_andheri_20.webp",
            imageType = GarageImageType.CURATED_WORKSHOP,
            sourceOrStatus = "Mechanic repairing car engine in workshop bay by Jwale2 (Wikimedia Commons)",
            legalNotice = "Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)"
        )
    )

    /**
     * Resolves the drawable resource ID for any garage ID.
     * Returns R.drawable.garage_default if the ID is missing, null, or unknown.
     */
    @DrawableRes
    fun getGarageImageRes(garageId: String?): Int {
        if (garageId.isNullOrBlank()) return DEFAULT_IMAGE_RES
        return registry[garageId]?.drawableRes ?: DEFAULT_IMAGE_RES
    }

    /**
     * Returns the full metadata for a given garage ID, or default fallback metadata.
     */
    fun getMetadata(garageId: String?): GarageImageMetadata {
        if (garageId.isNullOrBlank()) return defaultMetadata
        return registry[garageId] ?: defaultMetadata.copy(garageId = garageId)
    }

    /**
     * Returns the image type for a garage ID.
     */
    fun getImageType(garageId: String?): GarageImageType {
        return getMetadata(garageId).imageType
    }

    /**
     * Full list of all registered garage image entries for audits and verification.
     */
    val allGarages: List<GarageImageMetadata> = registry.values.toList()

    private val defaultMetadata = GarageImageMetadata(
        garageId = "garage_default",
        garageName = "AutoCare Workshop",
        drawableRes = DEFAULT_IMAGE_RES,
        filename = "garage_default.webp",
        imageType = GarageImageType.DEFAULT_FALLBACK,
        sourceOrStatus = "Modern multi-bay automotive service facility by S. M. Samee (Wikimedia Commons)",
        legalNotice = "Creative Commons CC0 1.0 Universal Public Domain Dedication"
    )
}

/**
 * Reusable Jetpack Compose Image composable for displaying a garage's picture.
 * Ensures consistent aspect ratio, cropping, and guaranteed offline fallback.
 */
@Composable
fun GarageImage(
    garageId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val resId = GarageImageProvider.getGarageImageRes(garageId)
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription ?: "Garage Image",
        contentScale = contentScale,
        modifier = modifier.fillMaxSize()
    )
}
