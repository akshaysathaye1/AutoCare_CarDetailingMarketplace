package com.example.mad_project_akshaysathaye_c049.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "customer", // "customer" or "garage_owner"
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
