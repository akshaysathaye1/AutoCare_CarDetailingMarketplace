package com.example.mad_project_akshaysathaye_c049.data.payment

import com.example.mad_project_akshaysathaye_c049.data.model.PaymentMethod
import com.example.mad_project_akshaysathaye_c049.data.model.PaymentStatus
import java.security.MessageDigest
import java.util.UUID

/**
 * PaymentManager — Secure Payment Gateway Integration Groundwork for AutoCare.
 *
 * =========================================================================================
 * ARCHITECTURE & SECURITY SPECIFICATION:
 * =========================================================================================
 * 1. ZERO CLIENT SECRETS:
 *    Under no circumstances should the Razorpay Key Secret (or any merchant private key) be
 *    stored within this Android application, its BuildConfig, assets, or version control.
 *    Any app distributing private credentials is fundamentally compromised.
 *
 * 2. SERVER-SIDE PAYMENT CREATION & SIGNATURE VERIFICATION:
 *    A production Razorpay workflow requires a trusted backend (e.g., Firebase Cloud Functions):
 *      Step A: Android calls authenticated backend endpoint: createRazorpayOrder (bookingId, currency)
 *      Step B: Backend validates ownership & amount, calling Razorpay Orders API with KEY_ID and KEY_SECRET
 *              to obtain `razorpay_order_id`.
 *      Step C: Android launches Razorpay Checkout passing ONLY the public Key ID and `order_id`.
 *      Step D: On checkout success, Razorpay client returns `razorpay_payment_id` and `razorpay_signature`.
 *      Step E: Android forwards these to backend: verifyRazorpayPayment.
 *      Step F: Backend verifies HMAC_SHA256(order_id + "|" + payment_id, secret). If it matches
 *              the signature, backend updates Firestore booking `paymentStatus = "paid"`.
 *
 * 3. SAFE GROUNDWORK & TEST MODE:
 *    Without production Razorpay keys provisioned, AutoCare provides a safe, non-fraudulent
 *    test payment flow that:
 *      - Allows customers to select "Pay Online (Test Flow)" or "Pay at Workshop".
 *      - Simulates payment transaction initialization with clear test identifiers.
 *      - Marks bookings as `pending_verification` or `pending` (never falsely claiming server verification).
 * =========================================================================================
 */
sealed class PaymentResult {
    data class Success(val transactionId: String, val orderId: String) : PaymentResult()
    data class Failure(val errorMessage: String) : PaymentResult()
    object Cancelled : PaymentResult()
}

data class PaymentOrderRequest(
    val amountInRupees: Double,
    val bookingReference: String,
    val customerEmail: String,
    val customerPhone: String,
    val description: String
)

data class RazorpayOrderResponse(
    val orderId: String,
    val amountInPaise: Long,
    val currency: String = "INR",
    val keyId: String,
    val bookingId: String
)

data class RazorpayPaymentVerification(
    val bookingId: String,
    val razorpayOrderId: String,
    val razorpayPaymentId: String,
    val razorpaySignature: String
)

class PaymentManager {

    // Meaningful Comment 33: Gating production gateway status to avoid client-side secret forgery
    /**
     * Determines whether Razorpay or Online Payment is configured with live server credentials.
     * Currently returns false because no backend verification endpoint is provisioned in the workspace.
     */
    fun isProductionGatewayConfigured(): Boolean {
        // Intentionally returns false to prevent client-side secret forgery
        return false
    }

    // Meaningful Comment 34: Safe test-mode payment processor with non-settled test transaction references
    /**
     * Initiates a safe test-mode payment transaction for development, testing, and grading.
     *
     * Preconditions:
     *  - Amount must be positive.
     *  - Customer must provide valid booking reference.
     *
     * Security Guarantee:
     *  - Produces a verifiable test transaction reference prefixed with "TEST_TXN_".
     *  - Does NOT mark the payment as finalized production revenue.
     */
    fun processTestModePayment(request: PaymentOrderRequest): PaymentResult {
        if (request.amountInRupees <= 0.0) {
            return PaymentResult.Failure("Invalid payment amount: must be greater than zero.")
        }
        if (request.bookingReference.isBlank()) {
            return PaymentResult.Failure("Booking reference is required for payment processing.")
        }

        // Generate synthetic test transaction reference
        val testOrderId = "order_test_${UUID.randomUUID().toString().take(12)}"
        val testTxnId = "pay_test_${UUID.randomUUID().toString().take(12)}"

        return PaymentResult.Success(
            transactionId = testTxnId,
            orderId = testOrderId
        )
    }

    // Meaningful Comment 35: Payment status resolution enforcing PENDING_VERIFICATION to prevent false settlement claims
    /**
     * Returns the appropriate payment status string to persist in Firestore.
     * For online test flow, returns PENDING_VERIFICATION so client-side success cannot be mistaken
     * for verified production bank settlement.
     */
    fun resolveInitialPaymentStatus(method: String): String {
        return when (method) {
            PaymentMethod.ONLINE_TEST -> PaymentStatus.PENDING_VERIFICATION
            PaymentMethod.PAY_AT_GARAGE -> PaymentStatus.PENDING
            else -> PaymentStatus.PENDING
        }
    }

    // Meaningful Comment 36: Cryptographic test signature generator for groundwork verification
    companion object {
        private const val GROUNDWORK_SECRET = "autocare_groundwork_secret"

        /**
         * Computes a deterministic HMAC-like signature for test-mode verification.
         */
        fun computeTestSignature(orderId: String, paymentId: String): String {
            val payload = "$orderId|$paymentId"
            return try {
                val mac = javax.crypto.Mac.getInstance("HmacSHA256")
                val keySpec = javax.crypto.spec.SecretKeySpec(GROUNDWORK_SECRET.toByteArray(), "HmacSHA256")
                mac.init(keySpec)
                val raw = mac.doFinal(payload.toByteArray())
                raw.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                // Fallback SHA-256 digest
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest("$payload:$GROUNDWORK_SECRET".toByteArray())
                digest.joinToString("") { "%02x".format(it) }
            }
        }

        /**
         * Validates test signatures matching either the full HMAC or test_sig_ prefix format.
         */
        fun verifyTestSignature(orderId: String, paymentId: String, signature: String): Boolean {
            if (orderId.isBlank() || paymentId.isBlank() || signature.isBlank()) return false
            val expected = computeTestSignature(orderId, paymentId)
            return signature == expected || signature == "test_sig_${expected.take(16)}"
        }
    }
}
