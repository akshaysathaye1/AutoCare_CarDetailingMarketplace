package com.example.mad_project_akshaysathaye_c049.data.model

data class Booking(
    val id: String = "",
    val customerId: String = "",
    val garageId: String = "",
    val serviceId: String = "",
    val vehicleId: String = "",
    val status: String = "pending", // pending, confirmed, in_progress, completed, cancelled
    val scheduledDate: Long = 0L,
    val scheduledTime: String = "",
    val totalAmount: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
