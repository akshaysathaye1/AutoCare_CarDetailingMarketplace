import functions from 'firebase-functions';
import admin from 'firebase-admin';
import { handleSlotReservation, handleSlotCancellation } from './reservation.mjs';
import { handleReviewWritten } from './review-aggregator.mjs';
import {
  handleCreateRazorpayOrder,
  handleVerifyRazorpayPayment,
  handleRazorpayWebhook
} from './razorpay-payment.mjs';
import { handleBookingStatusNotification } from './notifications.mjs';

// Initialize Admin SDK once for trusted backend execution
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const messaging = admin.messaging();

// Server-side Razorpay configuration (Loaded from environment / functions config)
const getRazorpayConfig = () => ({
  keyId: process.env.RAZORPAY_KEY_ID || functions.config().razorpay?.key_id || '',
  keySecret: process.env.RAZORPAY_KEY_SECRET || functions.config().razorpay?.key_secret || ''
});

/**
 * Callable Cloud Function: reserveBookingSlot
 * Atomically reserves a garage time slot and writes the customer's booking.
 */
export const reserveBookingSlot = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'The function must be called while authenticated.'
    );
  }

  try {
    const result = await handleSlotReservation(db, context.auth.uid, data);
    return result;
  } catch (error) {
    const msg = error.message || 'Failed to reserve booking slot.';
    if (msg.includes('SLOT_ALREADY_RESERVED')) {
      throw new functions.https.HttpsError('already-exists', msg);
    }
    if (msg.includes('PERMISSION_DENIED') || msg.includes('UNAUTHORIZED')) {
      throw new functions.https.HttpsError('permission-denied', msg);
    }
    if (msg.includes('NOT_FOUND') || msg.includes('MISMATCH')) {
      throw new functions.https.HttpsError('invalid-argument', msg);
    }
    throw new functions.https.HttpsError('internal', msg);
  }
});

/**
 * Callable Cloud Function: cancelBookingReservation
 * Atomically cancels a booking and releases its reserved slot.
 */
export const cancelBookingReservation = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'The function must be called while authenticated.'
    );
  }

  const { bookingId, cancellationReason = '' } = data;
  try {
    const result = await handleSlotCancellation(db, context.auth.uid, bookingId, cancellationReason);
    return result;
  } catch (error) {
    const msg = error.message || 'Failed to cancel booking reservation.';
    if (msg.includes('PERMISSION_DENIED')) {
      throw new functions.https.HttpsError('permission-denied', msg);
    }
    if (msg.includes('ILLEGAL_STATE') || msg.includes('NOT_FOUND')) {
      throw new functions.https.HttpsError('failed-precondition', msg);
    }
    throw new functions.https.HttpsError('internal', msg);
  }
});

/**
 * Cloud Firestore Trigger: onReviewWritten
 * Recalculates average rating and review count whenever a review is created, edited, or removed.
 */
export const onReviewWritten = functions.firestore
  .document('reviews/{reviewId}')
  .onWrite(async (change, context) => {
    try {
      return await handleReviewWritten(db, change);
    } catch (error) {
      console.error(`Error aggregating reviews for event ${context.eventId}:`, error);
      throw error;
    }
  });

/**
 * Callable Cloud Function: createRazorpayOrder
 * Authenticates customer, verifies booking ownership and status, and initiates a Razorpay order.
 * Key Secret is kept exclusively on the server.
 */
export const createRazorpayOrder = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'Authentication is required to initiate a payment order.'
    );
  }

  try {
    const config = getRazorpayConfig();
    return await handleCreateRazorpayOrder(db, context.auth.uid, data, config);
  } catch (error) {
    const msg = error.message || 'Failed to create payment order.';
    if (msg.includes('PERMISSION_DENIED')) {
      throw new functions.https.HttpsError('permission-denied', msg);
    }
    if (msg.includes('BOOKING_NOT_FOUND')) {
      throw new functions.https.HttpsError('not-found', msg);
    }
    if (msg.includes('ALREADY_PAID') || msg.includes('INVALID_BOOKING_STATUS')) {
      throw new functions.https.HttpsError('failed-precondition', msg);
    }
    if (msg.includes('INVALID_AMOUNT') || msg.includes('UNSUPPORTED_CURRENCY')) {
      throw new functions.https.HttpsError('invalid-argument', msg);
    }
    throw new functions.https.HttpsError('internal', msg);
  }
});

/**
 * Callable Cloud Function: verifyRazorpayPayment
 * Cryptographically verifies Razorpay payment signature and marks booking as paid.
 */
export const verifyRazorpayPayment = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'Authentication is required to verify payment.'
    );
  }

  try {
    const config = getRazorpayConfig();
    return await handleVerifyRazorpayPayment(db, context.auth.uid, data, config);
  } catch (error) {
    const msg = error.message || 'Payment verification failed.';
    if (msg.includes('SIGNATURE_VERIFICATION_FAILED')) {
      throw new functions.https.HttpsError('invalid-argument', msg);
    }
    if (msg.includes('PERMISSION_DENIED')) {
      throw new functions.https.HttpsError('permission-denied', msg);
    }
    if (msg.includes('ORDER_MISMATCH') || msg.includes('BOOKING_NOT_FOUND')) {
      throw new functions.https.HttpsError('failed-precondition', msg);
    }
    throw new functions.https.HttpsError('internal', msg);
  }
});

/**
 * HTTPS Webhook: razorpayWebhook
 * Receives and processes asynchronous Razorpay payment events (e.g., payment.captured, payment.failed).
 */
export const razorpayWebhook = functions.https.onRequest(async (req, res) => {
  if (req.method !== 'POST') {
    res.status(405).send('Method Not Allowed');
    return;
  }

  const webhookSignature = req.headers['x-razorpay-signature'];
  const webhookSecret = process.env.RAZORPAY_WEBHOOK_SECRET || functions.config().razorpay?.webhook_secret || '';

  if (!webhookSignature || !webhookSecret) {
    res.status(400).send('Webhook signature or server secret missing');
    return;
  }

  try {
    const rawBody = req.rawBody ? req.rawBody.toString('utf8') : JSON.stringify(req.body);
    const result = await handleRazorpayWebhook(db, rawBody, webhookSignature, webhookSecret);
    res.status(200).json(result);
  } catch (error) {
    console.error('Razorpay webhook processing error:', error);
    res.status(400).json({ error: error.message || 'Webhook processing failed' });
  }
});

/**
 * Cloud Firestore Trigger: onBookingStatusChanged
 * Dispatches push notification to customer's registered device upon status transitions.
 * Deduplicates notifications and handles stale token removal.
 */
export const onBookingStatusChanged = functions.firestore
  .document('bookings/{bookingId}')
  .onUpdate(async (change, context) => {
    try {
      return await handleBookingStatusNotification(db, messaging, change);
    } catch (error) {
      console.error(`Error processing booking notification for event ${context.eventId}:`, error);
      throw error;
    }
  });
