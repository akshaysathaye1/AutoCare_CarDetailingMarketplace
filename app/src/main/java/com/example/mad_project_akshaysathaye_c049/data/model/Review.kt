package com.example.mad_project_akshaysathaye_c049.data.model

data class Review(
    val id: String = "",
    val bookingId: String = "",
    val garageId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
