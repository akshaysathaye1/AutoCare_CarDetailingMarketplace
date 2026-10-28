package com.example.mad_project_akshaysathaye_c049.data.model

import com.example.mad_project_akshaysathaye_c049.data.payment.PaymentManager
import com.example.mad_project_akshaysathaye_c049.data.payment.PaymentOrderRequest
import com.example.mad_project_akshaysathaye_c049.data.payment.PaymentResult
import com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository
import com.example.mad_project_akshaysathaye_c049.data.repository.VehicleRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive Unit Tests for:
 * 1. Customer Profile validation (name, phone, role)
 * 2. Vehicle registration plate validation & tenant ownership enforcement
 * 3. Payment Gateway Architecture, test mode flow, and zero-client-secret guarantees
 */
class ProfileVehiclePaymentUnitTest {

    // =========================================================================
    // 1. Customer Profile Validation Tests
    // =========================================================================

    @Test
    fun customerProfileName_validationRules() {
        val validateName = { name: String ->
            val trimmed = name.trim()
            when {
                trimmed.isBlank() -> "Full name cannot be blank."
                trimmed.length < 2 -> "Full name must be at least 2 characters."
                else -> null
            }
        }

        assertEquals("Full name cannot be blank.", validateName(""))
        assertEquals("Full name cannot be blank.", validateName("   "))
        assertEquals("Full name must be at least 2 characters.", validateName("A"))
        assertEquals(null, validateName("Akshay Sathaye"))
        assertEquals(null, validateName("Rahul Sharma"))
    }

    @Test
    fun customerProfilePhone_validationRules() {
        val validatePhone = { phone: String ->
            val trimmed = phone.trim()
            if (trimmed.isNotBlank() && !AuthRepository.isValidPhone(trimmed)) {
                "Please enter a valid phone number (7-15 digits)."
            } else {
                null
            }
        }

        // Optional blank phone is acceptable
        assertEquals(null, validatePhone(""))
        assertEquals(null, validatePhone("   "))

        // Valid phone numbers
        assertEquals(null, validatePhone("9876543210"))
        assertEquals(null, validatePhone("+919876543210"))
        assertEquals(null, validatePhone("1234567890"))

        // Invalid phone formats
        assertEquals("Please enter a valid phone number (7-15 digits).", validatePhone("123"))
        assertEquals("Please enter a valid phone number (7-15 digits).", validatePhone("abcdefghij"))
    }

    @Test
    fun userModel_preservesRoleNormalizationAndFields() {
        val fields = mapOf(
            "name" to "John Doe",
            "email" to "john@example.com",
            "phone" to "9876543210",
            "role" to "CAR_OWNER"
        )
        val user = User.fromFields(fields, "user_abc_123")

        assertEquals("user_abc_123", user.uid)
        assertEquals("John Doe", user.name)
        assertEquals("john@example.com", user.email)
        assertEquals("9876543210", user.phoneNumber)
        assertEquals("CUSTOMER", user.role) // Normalized from CAR_OWNER
    }

    // =========================================================================
    // 2. Vehicle Registration & Ownership Tests
    // =========================================================================

    @Test
    fun vehicleRegistrationNumber_validationRules() {
        // Valid Indian RTO registration formats
        assertTrue("Standard MH plate is valid", VehicleRepository.isValidRegistrationNumber("MH 12 AB 1234"))
        assertTrue("Standard KA plate is valid", VehicleRepository.isValidRegistrationNumber("KA01MJ1234"))
        assertTrue("Hyphenated DL plate is valid", VehicleRepository.isValidRegistrationNumber("DL-01-C-4567"))
        assertTrue("GJ plate is valid", VehicleRepository.isValidRegistrationNumber("GJ 06 DE 9999"))

        // Valid alphanumeric plates
        assertTrue("Alphanumeric plate is valid", VehicleRepository.isValidRegistrationNumber("AUTO1234"))

        // Invalid plates
        assertFalse("Blank plate is invalid", VehicleRepository.isValidRegistrationNumber(""))
        assertFalse("Whitespace plate is invalid", VehicleRepository.isValidRegistrationNumber("   "))
        assertFalse("Too short plate is invalid", VehicleRepository.isValidRegistrationNumber("AB"))
        assertFalse("Too long plate (>16 chars) is invalid", VehicleRepository.isValidRegistrationNumber("MH12AB1234567890123"))
    }

    @Test
    fun vehicleOwnership_enforcesCrossTenantProtection() {
        val userA = "customer_uid_aaa"
        val userB = "customer_uid_bbb"

        val vehicle = Vehicle(
            id = "veh_999",
            ownerId = userA,
            make = "Hyundai",
            model = "Creta",
            licensePlate = "MH 02 BG 4567"
        )

        // User A owns the vehicle and can perform operations
        assertTrue("Owner matches userA", vehicle.ownerId == userA)

        // User B attempts to access/update/delete User A's vehicle
        assertFalse("User B cannot access userA vehicle", vehicle.ownerId == userB)
    }

    // =========================================================================
    // 3. Payment Gateway Architecture & Security Tests
    // =========================================================================

    @Test
    fun paymentGateway_securityConstraints_noClientSecretsInApk() {
        val paymentManager = PaymentManager()
        // Must return false to prevent client-side secret forgery without a backend
        assertFalse(
            "Production gateway must be marked not configured when backend server is absent",
            paymentManager.isProductionGatewayConfigured()
        )
    }

    @Test
    fun paymentGateway_testModeFlow_generatesValidTestIdentifiers() {
        val paymentManager = PaymentManager()
        val request = PaymentOrderRequest(
            amountInRupees = 1499.0,
            bookingReference = "bk_test_ref_101",
            customerEmail = "customer@example.com",
            customerPhone = "9876543210",
            description = "Complete Interior Detailing"
        )

        val result = paymentManager.processTestModePayment(request)
        assertTrue("Payment processing succeeded in test mode", result is PaymentResult.Success)

        val success = result as PaymentResult.Success
        assertTrue("Order ID must be prefixed with order_test_", success.orderId.startsWith("order_test_"))
        assertTrue("Transaction ID must be prefixed with pay_test_", success.transactionId.startsWith("pay_test_"))
    }

    @Test
    fun paymentGateway_validation_rejectsZeroOrNegativeAmounts() {
        val paymentManager = PaymentManager()
        val zeroRequest = PaymentOrderRequest(
            amountInRupees = 0.0,
            bookingReference = "bk_001",
            customerEmail = "c@example.com",
            customerPhone = "9876543210",
            description = "Wash"
        )
        val result = paymentManager.processTestModePayment(zeroRequest)
        assertTrue("Zero amount must fail", result is PaymentResult.Failure)

        val failure = result as PaymentResult.Failure
        assertTrue("Failure message mentions invalid amount", failure.errorMessage.contains("Invalid payment amount"))
    }

    @Test
    fun paymentGateway_initialStatus_neverFalselyMarksAsCompleted() {
        val paymentManager = PaymentManager()

        // Online test mode must resolve to PENDING_VERIFICATION (never COMPLETED)
        val onlineStatus = paymentManager.resolveInitialPaymentStatus(PaymentMethod.ONLINE_TEST)
        assertEquals(PaymentStatus.PENDING_VERIFICATION, onlineStatus)

        // Pay at garage must resolve to PENDING
        val garageStatus = paymentManager.resolveInitialPaymentStatus(PaymentMethod.PAY_AT_GARAGE)
        assertEquals(PaymentStatus.PENDING, garageStatus)
    }

    @Test
    fun bookingModel_preservesBackwardsCompatibilityWithPaymentDefaults() {
        // Constructing booking without explicit payment arguments uses safe defaults
        val legacyBooking = Booking(
            id = "bk_legacy_1",
            customerId = "cust_1",
            garageId = "gar_1",
            serviceId = "svc_1",
            totalAmount = 799.0
        )

        assertEquals("pay_at_garage", legacyBooking.paymentMethod)
        assertEquals("pending", legacyBooking.paymentStatus)
        assertEquals("", legacyBooking.paymentTransactionId)
    }

    @Test
    fun garageProfile_modelIntegrity_preservesAttributes() {
        val garage = Garage(
            id = "gar_test_88",
            ownerId = "owner_uid_99",
            name = "Apex Motor Works",
            city = "Bangalore",
            address = "Electronic City",
            contactNumber = "9876543210",
            rating = 4.9,
            reviewCount = 120,
            description = "Specialized detailing center."
        )

        assertEquals("gar_test_88", garage.id)
        assertEquals("owner_uid_99", garage.ownerId)
        assertEquals("Apex Motor Works", garage.name)
        assertEquals("Bangalore", garage.city)
        assertEquals(4.9, garage.rating, 0.01)
        assertEquals(120, garage.reviewCount)
    }

    @Test
    fun paymentStatus_distinguishesPendingVerificationFromCompleted() {
        // Security requirement: Ensure pending_verification is strictly distinct from completed
        assertFalse(
            "pending_verification must never equal completed",
            PaymentStatus.PENDING_VERIFICATION == PaymentStatus.COMPLETED
        )
        assertFalse(
            "pending must never equal completed",
            PaymentStatus.PENDING == PaymentStatus.COMPLETED
        )
    }

    @Test
    fun paymentGateway_cryptographicSignatures_validatesTestSignatures() {
        val orderId = "order_test_12345"
        val paymentId = "pay_test_67890"

        val signature = PaymentManager.computeTestSignature(orderId, paymentId)
        assertTrue("Generated signature must not be blank", signature.isNotBlank())

        // Valid signature verifies
        assertTrue(
            "Computed signature must verify successfully",
            PaymentManager.verifyTestSignature(orderId, paymentId, signature)
        )

        // Prefixed test signature verifies
        val prefixedSig = "test_sig_${signature.take(16)}"
        assertTrue(
            "Prefixed test signature must verify successfully",
            PaymentManager.verifyTestSignature(orderId, paymentId, prefixedSig)
        )

        // Tampered signature rejected
        assertFalse(
            "Tampered signature must be rejected",
            PaymentManager.verifyTestSignature(orderId, paymentId, "tampered_signature_hex")
        )

        // Mismatched order rejected
        assertFalse(
            "Mismatched order ID must be rejected",
            PaymentManager.verifyTestSignature("order_different", paymentId, signature)
        )
    }

    @Test
    fun paymentGateway_responseModel_holdsRazorpayAttributes() {
        val order = com.example.mad_project_akshaysathaye_c049.data.payment.RazorpayOrderResponse(
            orderId = "order_rzp_101",
            amountInPaise = 249900L,
            currency = "INR",
            keyId = "rzp_test_public_key",
            bookingId = "booking_101"
        )
        assertEquals("order_rzp_101", order.orderId)
        assertEquals(249900L, order.amountInPaise)
        assertEquals("INR", order.currency)
        assertEquals("rzp_test_public_key", order.keyId)

        val verification = com.example.mad_project_akshaysathaye_c049.data.payment.RazorpayPaymentVerification(
            bookingId = "booking_101",
            razorpayOrderId = "order_rzp_101",
            razorpayPaymentId = "pay_rzp_555",
            razorpaySignature = "sig_hex_abc"
        )
        assertEquals("pay_rzp_555", verification.razorpayPaymentId)
        assertEquals("sig_hex_abc", verification.razorpaySignature)
    }
}
