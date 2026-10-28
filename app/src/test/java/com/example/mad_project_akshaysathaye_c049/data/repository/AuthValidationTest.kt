package com.example.mad_project_akshaysathaye_c049.data.repository

import com.example.mad_project_akshaysathaye_c049.data.model.User
import com.example.mad_project_akshaysathaye_c049.ui.auth.sanitizeAuthError
import com.example.mad_project_akshaysathaye_c049.ui.auth.sanitizeRegisterError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying AuthRepository input validation, error mapping, and partial registration safety rules.
 */
class AuthValidationTest {

    private val authRepository = AuthRepository()

    @Test
    fun validEmail_passesValidation() {
        assertTrue(authRepository.isValidEmail("user@autocare.com"))
        assertTrue(authRepository.isValidEmail("customer.test@domain.co.in"))
    }

    @Test
    fun invalidEmail_failsValidation() {
        assertFalse(authRepository.isValidEmail("invalid-email"))
        assertFalse(authRepository.isValidEmail("user@"))
        assertFalse(authRepository.isValidEmail("@domain.com"))
        assertFalse(authRepository.isValidEmail(""))
    }

    @Test
    fun validPhone_passesValidation() {
        assertTrue(authRepository.isValidPhone("+91 9876543210"))
        assertTrue(authRepository.isValidPhone("9876543210"))
        assertTrue(authRepository.isValidPhone("(080) 2852-9999"))
    }

    @Test
    fun invalidPhone_failsValidation() {
        assertFalse(authRepository.isValidPhone("123")) // Too short (< 7 chars)
        assertFalse(authRepository.isValidPhone("abc-def-ghij")) // Alphabetic
        assertFalse(authRepository.isValidPhone("")) // Empty
    }

    @Test
    fun emptyFields_returnsRequiredError() {
        var isSuccess = false
        var errorMsg: String? = null

        authRepository.registerUser(
            name = "",
            email = "user@autocare.com",
            password = "password123",
            phone = "9876543210",
            role = "CUSTOMER"
        ) { success, error ->
            isSuccess = success
            errorMsg = error
        }

        assertFalse(isSuccess)
        assertEquals("Please fill in all the required fields.", errorMsg)
    }

    @Test
    fun shortPassword_returnsLengthError() {
        var isSuccess = false
        var errorMsg: String? = null

        authRepository.registerUser(
            name = "Test Customer",
            email = "user@autocare.com",
            password = "123",
            phone = "9876543210",
            role = "CUSTOMER"
        ) { success, error ->
            isSuccess = success
            errorMsg = error
        }

        assertFalse(isSuccess)
        assertEquals("Password must be at least 6 characters long.", errorMsg)
    }

    @Test
    fun invalidEmailFormat_returnsEmailError() {
        var isSuccess = false
        var errorMsg: String? = null

        authRepository.registerUser(
            name = "Test Customer",
            email = "bademail",
            password = "password123",
            phone = "9876543210",
            role = "CUSTOMER"
        ) { success, error ->
            isSuccess = success
            errorMsg = error
        }

        assertFalse(isSuccess)
        assertEquals("Please enter a valid email address.", errorMsg)
    }

    @Test
    fun invalidRole_returnsRoleError() {
        var isSuccess = false
        var errorMsg: String? = null

        authRepository.registerUser(
            name = "Test User",
            email = "user@autocare.com",
            password = "password123",
            phone = "9876543210",
            role = "INVALID_ADMIN_ROLE"
        ) { success, error ->
            isSuccess = success
            errorMsg = error
        }

        assertFalse(isSuccess)
        assertEquals("Please select a valid account role.", errorMsg)
    }

    @Test
    fun roleNormalization_acceptsCustomerAndGarageVariants() {
        assertEquals("CUSTOMER", User.normalizeRole("CUSTOMER"))
        assertEquals("CUSTOMER", User.normalizeRole("customer"))
        assertEquals("CUSTOMER", User.normalizeRole("  Customer  "))

        assertEquals("GARAGE", User.normalizeRole("GARAGE"))
        assertEquals("GARAGE", User.normalizeRole("garage"))
        assertEquals("GARAGE", User.normalizeRole("GARAGE_OWNER"))
        assertEquals("GARAGE", User.normalizeRole("garage_owner"))
    }

    @Test
    fun roleNormalization_rejectsUnknownRoles() {
        assertNull(User.normalizeRole(null))
        assertNull(User.normalizeRole(""))
        assertNull(User.normalizeRole("ADMIN"))
        assertNull(User.normalizeRole("MODERATOR"))
        assertNull(User.normalizeRole("UNKNOWN"))
    }

    @Test
    fun loginEmptyFields_returnsValidationError() {
        var isSuccess = false
        var roleResult: String? = null
        var errorMsg: String? = null

        authRepository.loginUser(
            email = "",
            password = ""
        ) { success, role, error ->
            isSuccess = success
            roleResult = role
            errorMsg = error
        }

        assertFalse(isSuccess)
        assertNull(roleResult)
        assertEquals("Email and password cannot be empty.", errorMsg)
    }

    @Test
    fun mapAuthException_nullException_returnsDefaultMessage() {
        val msg = authRepository.mapAuthException(null)
        assertEquals("Authentication failed. Please check your credentials and try again.", msg)
    }

    @Test
    fun mapAuthException_invalidCredentialMessage_mapsUserFriendlyError() {
        val ex = Exception("The supplied auth credential is incorrect, malformed or has expired.")
        val msg = authRepository.mapAuthException(ex)
        assertEquals("Invalid email address or password. Please verify your credentials and try again.", msg)
    }

    @Test
    fun mapAuthException_userNotFoundMessage_mapsUserFriendlyError() {
        val ex = Exception("There is no user record corresponding to this identifier.")
        val msg = authRepository.mapAuthException(ex)
        assertEquals("No account found with this email address. Please check your email or register.", msg)
    }

    @Test
    fun mapAuthException_networkMessage_mapsNetworkError() {
        val ex = Exception("Network connection error timeout")
        val msg = authRepository.mapAuthException(ex)
        assertEquals("Network connection error. Please check your internet connection.", msg)
    }

    @Test
    fun mapAuthException_emailAlreadyInUse_mapsCollisionMessage() {
        val ex = Exception("The email address is already in use by another account.")
        val msg = authRepository.mapAuthException(ex)
        assertEquals("An account with this email address already exists. Please sign in instead.", msg)
    }

    @Test
    fun mapAuthException_firestoreDisabledMessage_mapsConsoleActionError() {
        val ex = Exception("Cloud Firestore API has not been used in project autocare-c049 before or it is disabled.")
        val msg = authRepository.mapAuthException(ex)
        assertEquals("Cloud Firestore API is disabled in Firebase Console. Please enable it in project autocare-c049.", msg)
    }

    @Test
    fun sanitizeRegisterError_distinguishesProfileTimeoutFromNetworkError() {
        val timeoutError = "Account created, but profile setup timed out. Cloud Firestore database may be disabled or unavailable in Firebase Console."
        val sanitized = sanitizeRegisterError(timeoutError)
        assertEquals(timeoutError, sanitized)
        assertFalse(sanitized.contains("Please check your internet connection"))
    }

    @Test
    fun sanitizeRegisterError_preservesFirestoreDisabledError() {
        val firestoreDisabled = "Cloud Firestore API is disabled in Firebase Console. Please enable it in project autocare-c049."
        val sanitized = sanitizeRegisterError(firestoreDisabled)
        assertEquals(firestoreDisabled, sanitized)
    }

    @Test
    fun sanitizeRegisterError_mapsEmailCollision() {
        val raw = "The email address is already exists or already in use"
        val sanitized = sanitizeRegisterError(raw)
        assertEquals("An account with this email address already exists. Please sign in instead.", sanitized)
    }

    @Test
    fun sanitizeRegisterError_mapsWeakPassword() {
        val raw = "The password is too weak."
        val sanitized = sanitizeRegisterError(raw)
        assertEquals("The password is too weak. Please use at least 6 characters.", sanitized)
    }

    @Test
    fun sanitizeRegisterError_mapsTrueNetworkFailure() {
        val raw = "com.google.firebase.FirebaseNetworkException: A network error (such as timeout, interrupted connection or unreachable host) has occurred."
        val sanitized = sanitizeRegisterError(raw)
        assertEquals("Network connection error. Please check your internet connection.", sanitized)
    }

    @Test
    fun sanitizeAuthError_preservesFirestoreError() {
        val raw = "Could not load your account profile: Cloud Firestore API is disabled in Firebase Console. Please enable it in project autocare-c049."
        val sanitized = sanitizeAuthError(raw)
        assertEquals(raw, sanitized)
    }

    @Test
    fun sanitizeRegisterError_preservesPermissionDeniedError() {
        val raw = "Database access permission denied. Please verify your account permissions."
        val sanitized = sanitizeRegisterError(raw)
        assertEquals(raw, sanitized)
    }

    @Test
    fun mapAuthException_permissionDenied_mapsPermissionError() {
        val ex = Exception("PERMISSION_DENIED: Missing or insufficient permissions.")
        val msg = authRepository.mapAuthException(ex)
        assertEquals("Database access permission denied. Please verify your account permissions.", msg)
    }
}
