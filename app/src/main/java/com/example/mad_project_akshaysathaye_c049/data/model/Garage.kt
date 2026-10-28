package com.example.mad_project_akshaysathaye_c049.data.model

data class Garage(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val address: String = "",
    val locality: String = "Andheri",
    val city: String = "Mumbai",
    val pincode: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val contactNumber: String = "",
    val priceRange: String = "₹₹",
    val imageUrls: List<String> = emptyList(),
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val isOpen: Boolean = true,
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val isDemoData: Boolean = true,
    val dataSource: String = "DEMO_CANDIDATE",
    val operatingHours: String = "09:00 AM - 07:00 PM",
    val createdAt: Long = System.currentTimeMillis()
)
