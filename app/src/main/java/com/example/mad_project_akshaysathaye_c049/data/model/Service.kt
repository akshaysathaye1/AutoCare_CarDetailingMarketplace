package com.example.mad_project_akshaysathaye_c049.data.model

data class Service(
    val id: String = "",
    val garageId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val durationMinutes: Int = 60,
    val category: String = "General Service",
    val imageUrl: String = "",
    val isAvailable: Boolean = true
)
