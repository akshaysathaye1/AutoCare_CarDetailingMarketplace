package com.example.mad_project_akshaysathaye_c049.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "CUSTOMER", // "CUSTOMER" or "GARAGE"
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun normalizeRole(rawRole: String?): String? {
            if (rawRole.isNullOrBlank()) return null
            val upper = rawRole.trim().uppercase()
            return when {
                upper == "CUSTOMER" || upper == "CAR_OWNER" || upper == "USER" -> "CUSTOMER"
                upper == "GARAGE" || upper == "GARAGE_OWNER" || upper == "WORKSHOP" -> "GARAGE"
                else -> null
            }
        }

        fun fromFields(fields: Map<String, Any?>, docId: String): User {
            val name = (fields["name"] as? String) ?: ""
            val email = (fields["email"] as? String) ?: ""
            val phone = (fields["phone"] as? String) ?: (fields["phoneNumber"] as? String) ?: ""
            val rawRole = (fields["role"] as? String) ?: "CUSTOMER"
            val normalizedRole = normalizeRole(rawRole) ?: "CUSTOMER"
            val photo = (fields["profileImageUrl"] as? String) ?: ""
            return User(
                uid = docId,
                name = name,
                email = email,
                role = normalizedRole,
                phoneNumber = phone,
                profileImageUrl = photo
            )
        }
    }
}
