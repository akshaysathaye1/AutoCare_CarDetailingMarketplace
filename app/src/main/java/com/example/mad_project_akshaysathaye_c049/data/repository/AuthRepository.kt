package com.example.mad_project_akshaysathaye_c049.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.example.mad_project_akshaysathaye_c049.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * AuthRepository manages User Authentication and Firestore Profile synchronization.
 *
 * What it is: A central manager (Repository) that communicates with Firebase Authentication
 *             and Cloud Firestore.
 * Why we need it: It abstracts database and network operations away from our UI Composables,
 *                 keeping our UI clean and easy to maintain.
 */
class AuthRepository(
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) {

    private val auth: FirebaseAuth by lazy { authProvider() }
    private val firestore: FirebaseFirestore by lazy { firestoreProvider() }

    object DiagnosticLogger {
        fun d(tag: String, msg: String) {
            try { android.util.Log.d(tag, msg) } catch (e: Throwable) { println("[$tag][DEBUG] $msg") }
        }
        fun i(tag: String, msg: String) {
            try { android.util.Log.i(tag, msg) } catch (e: Throwable) { println("[$tag][INFO] $msg") }
        }
        fun w(tag: String, msg: String) {
            try { android.util.Log.w(tag, msg) } catch (e: Throwable) { println("[$tag][WARN] $msg") }
        }
        fun e(tag: String, msg: String, t: Throwable? = null) {
            try { android.util.Log.e(tag, msg, t) } catch (e: Throwable) { println("[$tag][ERROR] $msg ${t?.message ?: ""}") }
        }
    }

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

    companion object {
        /**
         * Helper to validate email format safely across Android runtime and JVM unit tests.
         */
        fun isValidEmail(email: String): Boolean {
            val trimmed = email.trim()
            if (trimmed.isBlank()) return false
            return try {
                val pattern = android.util.Patterns.EMAIL_ADDRESS
                if (pattern != null) {
                    val matcher = pattern.matcher(trimmed)
                    if (matcher != null) matcher.matches() else isFallbackEmailValid(trimmed)
                } else {
                    isFallbackEmailValid(trimmed)
                }
            } catch (e: Throwable) {
                isFallbackEmailValid(trimmed)
            }
        }

        private fun isFallbackEmailValid(email: String): Boolean {
            val parts = email.split("@")
            if (parts.size != 2) return false
            val domain = parts[1]
            return parts[0].isNotBlank() && domain.contains(".") && !domain.startsWith(".") && !domain.endsWith(".")
        }

        /**
         * Validate phone number format (7-15 digits with optional country code, spaces, or hyphens).
         */
        fun isValidPhone(phone: String): Boolean {
            val trimmed = phone.trim()
            if (trimmed.length < 7 || trimmed.length > 15) return false
            return trimmed.matches(Regex("^[0-9+() -]+$"))
        }
    }

    fun isValidEmail(email: String): Boolean = AuthRepository.isValidEmail(email)
    fun isValidPhone(phone: String): Boolean = AuthRepository.isValidPhone(phone)

    /**
     * Map Cloud Firestore exceptions to user-facing messages distinguishing
     * disabled API / permission errors from transient service or timeout issues.
     */
    fun mapFirestoreException(exception: FirebaseFirestoreException): String {
        val msg = exception.localizedMessage ?: ""
        val lower = msg.lowercase()
        return when (exception.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                if (lower.contains("api has not been used") || lower.contains("disabled") || lower.contains("overview?project=")) {
                    "Cloud Firestore API is disabled in Firebase Console. Please enable it in project autocare-c049."
                } else {
                    "Database access permission denied. Please verify your account permissions."
                }
            }
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Cloud Firestore service is temporarily unavailable. Please try again later."
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Database request timed out while saving profile. Cloud Firestore may be unreachable."
            else ->
                "Database error (${exception.code}): ${exception.localizedMessage ?: "Unable to access database."}"
        }
    }

    /**
     * Map Firebase Authentication and Firestore exceptions to precise user-facing error messages.
     */
    fun mapAuthException(exception: Exception?): String {
        if (exception == null) return "Authentication failed. Please check your credentials and try again."

        if (exception is FirebaseFirestoreException) {
            return mapFirestoreException(exception)
        }

        if (exception is FirebaseAuthInvalidCredentialsException) {
            return "Invalid email address or password. Please verify your credentials and try again."
        }

        if (exception is FirebaseAuthInvalidUserException) {
            return when (exception.errorCode) {
                "ERROR_USER_DISABLED" -> "This user account has been disabled. Please contact AutoCare support."
                "ERROR_USER_NOT_FOUND" -> "No account found with this email address. Please check your email or register."
                else -> "No account found with this email address or the account is disabled."
            }
        }

        if (exception is FirebaseAuthUserCollisionException) {
            return "An account with this email address already exists. Please sign in instead."
        }

        if (exception is FirebaseNetworkException) {
            return "Network connection error. Please check your internet connection and try again."
        }

        if (exception is FirebaseAuthException) {
            return when (exception.errorCode) {
                "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_EMAIL" ->
                    "Invalid email address or password. Please verify your credentials and try again."
                "ERROR_USER_NOT_FOUND" ->
                    "No account found with this email address. Please check your email or register."
                "ERROR_USER_DISABLED" ->
                    "This user account has been disabled. Please contact AutoCare support."
                "ERROR_TOO_MANY_REQUESTS" ->
                    "Too many failed login attempts. Please wait a few minutes before trying again."
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    "Email/Password authentication is disabled in Firebase Console."
                "ERROR_EMAIL_ALREADY_IN_USE" ->
                    "An account with this email address already exists. Please sign in instead."
                "ERROR_WEAK_PASSWORD" ->
                    "The password is too weak. Please use at least 6 characters."
                else ->
                    exception.localizedMessage ?: "Authentication failed. Please verify your details."
            }
        }

        val msg = exception.localizedMessage ?: ""
        val lower = msg.lowercase()
        return when {
            lower.contains("credential") || lower.contains("password") || lower.contains("incorrect") || lower.contains("malformed") || lower.contains("expired") ->
                "Invalid email address or password. Please verify your credentials and try again."
            lower.contains("no user record") || lower.contains("user-not-found") ->
                "No account found with this email address. Please check your email or register."
            lower.contains("email-already-in-use") || lower.contains("already exists") || lower.contains("already in use") ->
                "An account with this email address already exists. Please sign in instead."
            lower.contains("weak-password") || lower.contains("weak password") ->
                "The password is too weak. Please use at least 6 characters."
            lower.contains("api has not been used") || (lower.contains("disabled") && lower.contains("firestore")) ->
                "Cloud Firestore API is disabled in Firebase Console. Please enable it in project autocare-c049."
            lower.contains("permission_denied") || lower.contains("permission denied") || lower.contains("insufficient permissions") ->
                "Database access permission denied. Please verify your account permissions."
            lower.contains("profile save failed") || lower.contains("profile setup") || lower.contains("cloud firestore") ->
                msg
            lower.contains("network connection error") || lower.contains("firebasenetworkexception") || lower.contains("unknownhostexception") ->
                "Network connection error. Please check your internet connection."
            lower.contains("timed out") || lower.contains("timeout") ->
                msg.ifBlank { "Operation timed out. Please try again." }
            else -> msg.ifBlank { "Authentication failed. Please verify your details." }
        }
    }

    /**
     * Register a new user using Firebase Authentication and save their profile & role to Cloud Firestore.
     * Uses synchronous validation and background coroutine execution with 15-second safety timeout.
     */
    fun registerUser(
        name: String,
        email: String,
        password: String,
        phone: String,
        role: String,
        onResult: (isSuccess: Boolean, errorMessage: String?) -> Unit
    ) {
        val tag = "AuthRepository"
        DiagnosticLogger.d(tag, "1. Registration started for email: ${email.trim()}, role: $role")

        // 1. Synchronous Field Validation
        if (name.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
            DiagnosticLogger.w(tag, "Registration failed: Missing required fields")
            onResult(false, "Please fill in all the required fields.")
            return
        }
        if (!isValidEmail(email)) {
            DiagnosticLogger.w(tag, "Registration failed: Invalid email format")
            onResult(false, "Please enter a valid email address.")
            return
        }
        if (!isValidPhone(phone)) {
            DiagnosticLogger.w(tag, "Registration failed: Invalid phone format")
            onResult(false, "Please enter a valid phone number (7-15 digits).")
            return
        }
        if (password.length < 6) {
            DiagnosticLogger.w(tag, "Registration failed: Password too short")
            onResult(false, "Password must be at least 6 characters long.")
            return
        }
        val normalizedRole = User.normalizeRole(role)
        if (normalizedRole == null) {
            DiagnosticLogger.w(tag, "Registration failed: Invalid role '$role'")
            onResult(false, "Please select a valid account role.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            registerUserSuspend(name, email, password, phone, normalizedRole, onResult)
        }
    }

    suspend fun registerUserSuspend(
        name: String,
        email: String,
        password: String,
        phone: String,
        role: String,
        onResult: (isSuccess: Boolean, errorMessage: String?) -> Unit
    ) {
        val tag = "AuthRepository"
        val normalizedRole = User.normalizeRole(role) ?: "CUSTOMER"
        var uid = ""
        var authStepCompleted = false

        try {
            withTimeout(15000L) { // 15-second safety timeout
                DiagnosticLogger.d(tag, "2. Firebase Authentication account creation started...")

                try {
                    val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                    uid = authResult.user?.uid ?: ""
                    authStepCompleted = true
                    DiagnosticLogger.d(tag, "2b. Firebase Authentication account creation completed. UID: $uid")
                    try {
                        (authResult.user ?: auth.currentUser)?.getIdToken(false)?.await()
                    } catch (tokenEx: Exception) {
                        if (tokenEx is kotlinx.coroutines.CancellationException) throw tokenEx
                        DiagnosticLogger.w(tag, "Could not pre-fetch ID token: ${tokenEx.message}")
                    }
                } catch (authException: Exception) {
                    if (authException is kotlinx.coroutines.CancellationException) throw authException
                    DiagnosticLogger.w(tag, "2c. Firebase Auth failed: ${authException.javaClass.simpleName} - ${authException.message}")
                    if (isEmailCollisionException(authException)) {
                        DiagnosticLogger.i(tag, "4. Email collision detected. Attempting partial registration recovery...")
                        try {
                            val signInResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
                            uid = signInResult.user?.uid ?: ""
                            authStepCompleted = true
                            DiagnosticLogger.i(tag, "4b. Recovery sign-in completed. UID: $uid")
                            try {
                                (signInResult.user ?: auth.currentUser)?.getIdToken(false)?.await()
                            } catch (tokenEx: Exception) {
                                if (tokenEx is kotlinx.coroutines.CancellationException) throw tokenEx
                                DiagnosticLogger.w(tag, "Could not pre-fetch ID token on recovery: ${tokenEx.message}")
                            }
                        } catch (signInEx: Exception) {
                            if (signInEx is kotlinx.coroutines.CancellationException) throw signInEx
                            DiagnosticLogger.w(tag, "Recovery sign-in failed: ${signInEx.message}")
                            throw authException
                        }
                    } else {
                        throw authException
                    }
                }

                if (uid.isBlank()) {
                    DiagnosticLogger.e(tag, "UID is blank after Auth step.")
                    logout()
                    onResult(false, "Failed to retrieve user ID after account creation.")
                    return@withTimeout
                }

                // 3. Firestore Profile Creation
                DiagnosticLogger.d(tag, "3. Firestore profile write started for UID: $uid")
                val userProfile = hashMapOf(
                    "userId" to uid,
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "phone" to phone.trim(),
                    "role" to normalizedRole
                )

                try {
                    firestore.collection("users").document(uid).set(userProfile).await()
                    DiagnosticLogger.d(tag, "3b. Firestore profile write completed successfully for $uid")
                    DiagnosticLogger.d(tag, "5. Invoking result callback with SUCCESS")
                    onResult(true, null)
                } catch (firestoreError: Exception) {
                    if (firestoreError is kotlinx.coroutines.CancellationException) {
                        throw firestoreError
                    }
                    DiagnosticLogger.e(tag, "3c. Firestore profile write failed: ${firestoreError.javaClass.simpleName} - ${firestoreError.message}", firestoreError)
                    logout()
                    val errorDetail = if (firestoreError is FirebaseFirestoreException) {
                        mapFirestoreException(firestoreError)
                    } else {
                        "Profile save failed: ${firestoreError.localizedMessage ?: "Unknown database error"}"
                    }
                    onResult(false, errorDetail)
                }
            }
        } catch (timeoutEx: kotlinx.coroutines.TimeoutCancellationException) {
            DiagnosticLogger.e(tag, "Registration timed out after 15 seconds.", timeoutEx)
            logout()
            if (authStepCompleted && uid.isNotBlank()) {
                onResult(
                    false,
                    "Account created, but profile setup timed out. Cloud Firestore database may be disabled or unavailable in Firebase Console."
                )
            } else {
                onResult(
                    false,
                    "Registration request timed out. Please check your internet connection and try again."
                )
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            DiagnosticLogger.e(tag, "Registration exception: ${e.javaClass.simpleName} - ${e.message}", e)
            val mappedMsg = mapAuthException(e)
            onResult(false, mappedMsg)
        }
    }

    private fun isEmailCollisionException(exception: Exception?): Boolean {
        if (exception == null) return false
        if (exception is FirebaseAuthUserCollisionException) return true
        if (exception is FirebaseAuthException && exception.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") return true
        val msg = exception.localizedMessage?.lowercase() ?: ""
        return msg.contains("email-already-in-use") || msg.contains("already exists") || msg.contains("already in use")
    }

    /**
     * Authenticate an existing user with Email and Password, and fetch their stored role from Firestore.
     */
    fun loginUser(
        email: String,
        password: String,
        onResult: (isSuccess: Boolean, role: String?, errorMessage: String?) -> Unit
    ) {
        val tag = "AuthRepository"
        DiagnosticLogger.d(tag, "Login started for email: ${email.trim()}")

        // 1. Basic Field Validation
        if (email.isBlank() || password.isBlank()) {
            DiagnosticLogger.w(tag, "Login failed: Missing email or password")
            onResult(false, null, "Email and password cannot be empty.")
            return
        }

        try {
            // 2. Authenticate with Firebase Auth
            auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { authTask ->
                    try {
                        if (authTask.isSuccessful) {
                            DiagnosticLogger.d(tag, "Firebase Authentication sign-in succeeded")
                            val uid = authTask.result?.user?.uid ?: ""
                            if (uid.isBlank()) {
                                DiagnosticLogger.e(tag, "Firebase Auth sign-in succeeded but UID was blank")
                                logout()
                                onResult(false, null, "Unable to verify your account. Please sign in again.")
                                return@addOnCompleteListener
                            }

                            // 3. Fetch User Role from Cloud Firestore
                            try {
                                firestore.collection("users").document(uid).get()
                                    .addOnCompleteListener { profileTask ->
                                        try {
                                            if (profileTask.isSuccessful) {
                                                val snapshot = profileTask.result
                                                val profile = snapshot?.data?.let { User.fromFields(it, uid) }
                                                val role = User.normalizeRole(profile?.role)
                                                if (role == null) {
                                                    DiagnosticLogger.w(tag, "Firestore profile missing or invalid role for UID $uid")
                                                    logout()
                                                    onResult(false, null, "Your account profile is missing or has an invalid role. Please contact support.")
                                                } else {
                                                    DiagnosticLogger.d(tag, "Login successful for UID $uid with role $role")
                                                    onResult(true, role, null)
                                                }
                                            } else {
                                                val exception = profileTask.exception
                                                val error = if (exception is FirebaseFirestoreException) {
                                                    mapFirestoreException(exception)
                                                } else {
                                                    exception?.localizedMessage ?: "Unknown error"
                                                }
                                                DiagnosticLogger.e(tag, "Failed to fetch user profile for UID $uid: $error")
                                                logout()
                                                onResult(false, null, "Could not load your account profile: $error")
                                            }
                                        } catch (e: Throwable) {
                                            DiagnosticLogger.e(tag, "Error processing profileTask: ${e.message}", e)
                                            logout()
                                            onResult(false, null, "Could not verify your account role. Please try again.")
                                        }
                                    }
                            } catch (e: Throwable) {
                                DiagnosticLogger.e(tag, "Exception fetching user profile for UID $uid: ${e.message}", e)
                                logout()
                                onResult(false, null, "Could not verify your account role. Please try again.")
                            }
                        } else {
                            val exception = authTask.exception
                            DiagnosticLogger.w(tag, "Firebase Authentication sign-in failed: ${exception?.message}")
                            val authError = mapAuthException(exception)
                            onResult(false, null, authError)
                        }
                    } catch (e: Throwable) {
                        DiagnosticLogger.e(tag, "Error inside loginUser completion listener: ${e.message}", e)
                        onResult(false, null, "Login error: ${e.localizedMessage}")
                    }
                }
        } catch (e: Throwable) {
            DiagnosticLogger.e(tag, "Unexpected exception during loginUser: ${e.message}", e)
            onResult(false, null, "Firebase service notice: ${e.localizedMessage}")
        }
    }

    /**
     * Fetch user profile from Firestore using the specified or current user ID.
     */
    fun getUserProfile(
        userId: String? = getCurrentUserId(),
        onResult: (user: User?, errorMessage: String?) -> Unit
    ) {
        val targetUid = userId?.trim()
        if (targetUid.isNullOrBlank()) {
            onResult(null, "No active user session found.")
            return
        }

        try {
            firestore.collection("users").document(targetUid).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val snapshot = task.result
                        if (snapshot != null && snapshot.exists() && snapshot.data != null) {
                            val profile = User.fromFields(snapshot.data!!, targetUid)
                            val normalizedRole = User.normalizeRole(profile.role)
                            if (normalizedRole == null) {
                                onResult(null, "Account profile has an invalid or missing role.")
                            } else {
                                onResult(profile, null)
                            }
                        } else {
                            onResult(null, "User profile not found.")
                        }
                    } else {
                        val error = task.exception?.localizedMessage ?: "Failed to load user profile."
                        onResult(null, error)
                    }
                }
        } catch (e: Throwable) {
            onResult(null, "Profile fetch notice: ${e.localizedMessage}")
        }
    }

    /**
     * Update non-sensitive profile fields (name, phone) for the current user.
     * Protected fields (userId, role, garage ownership) are strictly prohibited from client edits.
     */
    fun updateUserProfile(
        name: String,
        phone: String,
        onResult: (isSuccess: Boolean, errorMessage: String?) -> Unit
    ) {
        val uid = getCurrentUserId()
        if (uid.isNullOrBlank()) {
            onResult(false, "User is not authenticated.")
            return
        }

        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()

        if (trimmedName.isBlank()) {
            onResult(false, "Full name cannot be blank.")
            return
        }
        if (trimmedPhone.isNotBlank() && !isValidPhone(trimmedPhone)) {
            onResult(false, "Please enter a valid phone number.")
            return
        }

        val updates = hashMapOf<String, Any>(
            "name" to trimmedName,
            "phone" to trimmedPhone
        )

        try {
            firestore.collection("users").document(uid)
                .update(updates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        val error = task.exception?.localizedMessage ?: "Failed to update profile."
                        onResult(false, error)
                    }
                }
        } catch (e: Throwable) {
            onResult(false, "Profile update notice: ${e.localizedMessage}")
        }
    }

    /**
     * Suspend function to fetch user profile safely from Cloud Firestore.
     * Enforces that callers retrieve verified records matching their active session or requested UID.
     */
    suspend fun getUserProfileAsync(userId: String? = getCurrentUserId()): Result<User> {
        val targetUid = userId?.trim()
        if (targetUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("No active user session found."))
        }

        return try {
            val snapshot = firestore.collection("users").document(targetUid).get().await()
            if (snapshot != null && snapshot.exists() && snapshot.data != null) {
                val profile = User.fromFields(snapshot.data!!, targetUid)
                Result.success(profile)
            } else {
                Result.failure(NoSuchElementException("User profile record not found in Firestore."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Suspend function to update supported customer profile fields (full name, phone number).
     * Security Rule: Authenticated UID is extracted strictly from the active session.
     * Users can ONLY update their own document, preventing cross-tenant privilege escalation.
     */
    suspend fun updateUserProfileAsync(name: String, phone: String): Result<Unit> {
        val uid = getCurrentUserId()
        if (uid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not authenticated. Please log in."))
        }

        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()

        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Full name cannot be blank."))
        }
        if (trimmedName.length < 2) {
            return Result.failure(IllegalArgumentException("Full name must be at least 2 characters."))
        }
        if (trimmedPhone.isNotBlank() && !isValidPhone(trimmedPhone)) {
            return Result.failure(IllegalArgumentException("Please enter a valid phone number (7-15 digits)."))
        }

        val updates = hashMapOf<String, Any>(
            "name" to trimmedName,
            "phone" to trimmedPhone
        )

        return try {
            firestore.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Log out current Firebase session.
     */
    fun logout() {
        try {
            GarageRepository.clearCache()
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

    /**
     * Get the authenticated Firebase UID.
     * Returns an empty string when no active Firebase Auth session exists.
     * Callers must guard against empty strings before writing to Firestore.
     */
    fun getEffectiveUserId(defaultRole: String = "CUSTOMER"): String {
        return getCurrentUserId() ?: ""
    }
}
