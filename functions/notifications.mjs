/**
 * AutoCare Notification Formatting Helper.
 * Formats user-facing status notification strings for each booking lifecycle stage.
 * Never includes customer address, vehicle registration, or payment details in push payloads.
 */
export function formatNotificationPayload(status, garageName = '', serviceName = '') {
  const workshop = garageName.trim() || 'AutoCare Partner Workshop';
  const service = serviceName.trim() || 'Detailing Service';

  switch (status?.toLowerCase()) {
    case 'confirmed':
      return {
        title: 'Appointment Confirmed! 🚗',
        body: `${workshop} has confirmed your appointment for ${service}.`
      };
    case 'in_progress':
      return {
        title: 'Service Started 🛠️',
        body: `${workshop} has started detailing work on your vehicle.`
      };
    case 'completed':
      return {
        title: 'Service Completed! ✨',
        body: `Your vehicle is ready for pickup at ${workshop}. Tap to rate and review your experience.`
      };
    case 'cancelled':
      return {
        title: 'Booking Update ⚠️',
        body: `Your booking for ${service} at ${workshop} was cancelled.`
      };
    default:
      return null;
  }
}

/**
 * Cloud Firestore Trigger Handler for booking status changes.
 *
 * Guarantees:
 * 1. Only triggers when status actually changes to a notable state.
 * 2. Deduplicates notifications to prevent spam on multiple updates.
 * 3. Preserves privacy: payload contains only non-sensitive schedule coordination information.
 * 4. Cleans up stale / expired FCM tokens automatically.
 */
export async function handleBookingStatusNotification(db, messaging, change) {
  const beforeData = change.before?.exists ? change.before.data() : null;
  const afterData = change.after?.exists ? change.after.data() : null;

  if (!afterData) {
    return { skipped: true, reason: 'BOOKING_DELETED' };
  }

  const oldStatus = beforeData?.status || '';
  const newStatus = afterData.status || '';

  // Skip if status did not change
  if (oldStatus === newStatus) {
    return { skipped: true, reason: 'STATUS_UNCHANGED' };
  }

  // Format content for notable lifecycle states
  const formatted = formatNotificationPayload(newStatus, afterData.garageName, afterData.serviceName);
  if (!formatted) {
    return { skipped: true, reason: 'STATUS_NOT_NOTIFIED', status: newStatus };
  }

  // Deduplication check: prevent duplicate push for the same status
  if (afterData.lastNotifiedStatus === newStatus) {
    return { skipped: true, reason: 'ALREADY_NOTIFIED', status: newStatus };
  }

  const customerId = afterData.customerId;
  if (!customerId) {
    return { skipped: true, reason: 'NO_CUSTOMER_ID' };
  }

  const userRef = db.doc(`users/${customerId}`);
  const userDoc = await userRef.get();
  if (!userDoc.exists) {
    return { skipped: true, reason: 'CUSTOMER_PROFILE_NOT_FOUND', customerId };
  }

  const userData = userDoc.data();
  const fcmToken = userData.fcmToken;
  if (!fcmToken || typeof fcmToken !== 'string') {
    return { skipped: true, reason: 'NO_FCM_TOKEN', customerId };
  }

  const bookingId = change.after.id;

  // Non-PII data payload
  const message = {
    token: fcmToken,
    notification: {
      title: formatted.title,
      body: formatted.body
    },
    data: {
      bookingId: bookingId || '',
      status: newStatus,
      garageName: afterData.garageName || '',
      serviceName: afterData.serviceName || ''
    },
    android: {
      priority: 'high',
      notification: {
        channelId: 'autocare_booking_updates',
        priority: 'high',
        defaultSound: true,
        defaultVibrateTimings: true
      }
    }
  };

  try {
    if (messaging && typeof messaging.send === 'function') {
      await messaging.send(message);
    }

    // Record notification dispatch on booking to guarantee idempotency
    await change.after.ref.update({
      lastNotifiedStatus: newStatus,
      lastNotifiedAt: Date.now()
    });

    return {
      success: true,
      bookingId,
      status: newStatus,
      customerId
    };
  } catch (error) {
    const errorCode = error.code || error.errorInfo?.code || '';
    if (
      errorCode === 'messaging/invalid-registration-token' ||
      errorCode === 'messaging/registration-token-not-registered'
    ) {
      // Automatic stale token cleanup
      await userRef.update({
        fcmToken: null,
        fcmTokenInvalidatedAt: Date.now()
      });
      return { skipped: true, reason: 'STALE_TOKEN_CLEANED', customerId };
    }

    console.error(`Failed to send FCM notification for booking ${bookingId}:`, error);
    throw error;
  }
}
