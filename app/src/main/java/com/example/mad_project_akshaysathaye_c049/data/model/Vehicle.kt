package com.example.mad_project_akshaysathaye_c049.data.model

data class Vehicle(
    val id: String = "",
    val ownerId: String = "",
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val licensePlate: String = "",
    val type: String = "Car", // Car, Bike
    val color: String = ""
)
