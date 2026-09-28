package com.example.mad_project_akshaysathaye_c049.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * AuthRepository manages User Authentication and Firestore Profile synchronization.
 *
 * What it is: A central manager (Repository) that communicates with Firebase Authentication
 *             and Cloud Firestore.
 * Why we need it: It abstracts database and network operations away from our UI Composables,
 *                 keeping our UI clean and easy to maintain.
 */
class AuthRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Check if a user session is currently active.
     */
    fun isUserLoggedIn(): Boolean {
        return try {
            auth.currentUser != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Register a new user using Firebase Authentication and save their profile & role to Cloud Firestore.
     */
    fun registerUser(
        name: String,
        email: String,
        password: String,
        phone: String,
        role: String,
        onResult: (isSuccess: Boolean, errorMessage: String?) -> Unit
    ) {
        // 1. Basic Field Validation
        if (name.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
            onResult(false, "Please fill in all the required fields.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            onResult(false, "Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            onResult(false, "Password must be at least 6 characters long.")
            return
        }

        try {
            // 2. Create Firebase Authentication Account
            auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: ""
                    val userProfile = hashMapOf(
                        "userId" to uid,
                        "name" to name.trim(),
                        "email" to email.trim(),
                        "phone" to phone.trim(),
                        "role" to role
                    )

                    // 3. Save User Profile & Role in Firestore "users" collection
                    firestore.collection("users").document(uid)
                        .set(userProfile)
                        .addOnSuccessListener {
                            onResult(true, null)
                        }
                        .addOnFailureListener { firestoreError ->
                            // Account created in Auth, but Firestore write had an issue
                            onResult(true, "Account created, but profile sync noted: ${firestoreError.localizedMessage}")
                        }
                }
                .addOnFailureListener { authError ->
                    onResult(false, authError.localizedMessage ?: "Registration failed. Please check credentials.")
                }
        } catch (e: Exception) {
            // Graceful fallback for offline / mock testing before Firebase Console is linked
            onResult(false, "Firebase service notice: ${e.localizedMessage}")
        }
    }

    /**
     * Authenticate an existing user with Email and Password, and fetch their stored role from Firestore.
     */
    fun loginUser(
        email: String,
        password: String,
        onResult: (isSuccess: Boolean, role: String?, errorMessage: String?) -> Unit
    ) {
        // 1. Basic Field Validation
        if (email.isBlank() || password.isBlank()) {
            onResult(false, null, "Email and password cannot be empty.")
            return
        }

        try {
            // 2. Authenticate with Firebase Auth
            auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: ""

                    // 3. Fetch User Role from Cloud Firestore
                    firestore.collection("users").document(uid).get()
                        .addOnSuccessListener { documentSnapshot ->
                            val role = documentSnapshot.getString("role") ?: "CUSTOMER"
                            onResult(true, role, null)
                        }
                        .addOnFailureListener {
                            // If Firestore role fetch fails, default to CUSTOMER
                            onResult(true, "CUSTOMER", null)
                        }
                }
                .addOnFailureListener { authError ->
                    onResult(false, null, authError.localizedMessage ?: "Invalid email or password.")
                }
        } catch (e: Exception) {
            onResult(false, null, "Firebase service notice: ${e.localizedMessage}")
        }
    }

    /**
     * Log out current Firebase session.
     */
    fun logout() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            // Ignore if already signed out
        }
    }

    /**
     * Get Current User ID
     */
    fun getCurrentUserId(): String? {
        return try {
            auth.currentUser?.uid
        } catch (e: Exception) {
            null
        }
    }
}
