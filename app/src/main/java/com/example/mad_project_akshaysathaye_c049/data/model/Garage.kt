package com.example.mad_project_akshaysathaye_c049.data.model

data class Garage(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val address: String = "",
    val city: String = "",
    val contactNumber: String = "",
    val imageUrls: List<String> = emptyList(),
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val isOpen: Boolean = true,
    val operatingHours: String = "09:00 AM - 06:00 PM",
    val createdAt: Long = System.currentTimeMillis()
)
