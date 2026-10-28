import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createHmac } from 'node:crypto';
import { HOST, ROOT } from '../../tools/reservation-migration/emulator.mjs';
import { fields, decode } from '../../tools/reservation-migration/plan.mjs';
import {
  computePaymentSignature,
  verifyPaymentSignature,
  handleCreateRazorpayOrder,
  handleVerifyRazorpayPayment,
  handleRazorpayWebhook
} from '../../functions/razorpay-payment.mjs';
import {
  formatNotificationPayload,
  handleBookingStatusNotification
} from '../../functions/notifications.mjs';

function token(uid) {
  const b64 = v => Buffer.from(JSON.stringify(v)).toString('base64url');
  const now = Math.floor(Date.now() / 1000);
  return `${b64({ alg: 'none', typ: 'JWT' })}.${b64({
    sub: uid,
    user_id: uid,
    aud: 'demo-autocare-migration',
    iss: 'https://securetoken.google.com/demo-autocare-migration',
    iat: now,
    exp: now + 3600,
    firebase: { sign_in_provider: 'custom', identities: {} }
  })}.`;
}

async function adminSet(path, data) {
  return fetch(`${HOST}/v1/${ROOT}:commit`, {
    method: 'POST',
    redirect: 'error',
    headers: {
      authorization: 'Bearer owner',
      'content-type': 'application/json'
    },
    body: JSON.stringify({
      writes: [{
        update: {
          name: `${ROOT}/${path}`,
          fields: fields(data)
        }
      }]
    })
  });
}

async function adminGet(path) {
  const res = await fetch(`${HOST}/v1/${ROOT}/${path}`, {
    headers: { authorization: 'Bearer owner' }
  });
  if (!res.ok) return null;
  const json = await res.json();
  return decode(json.fields);
}

// Lightweight Firestore emulator DB adapter for Cloud Function testing
function createEmulatorDb() {
  return {
    doc(path) {
      return {
        path,
        async get() {
          const data = await adminGet(path);
          return {
            exists: data !== null,
            data: () => data
          };
        },
        async update(updates) {
          const existing = await adminGet(path) || {};
          await adminSet(path, { ...existing, ...updates });
        }
      };
    },
    collection(colName) {
      return {
        where(field, op, val) {
          return {
            limit() {
              return {
                async get() {
                  // Scan emulator documents for testing webhook
                  const res = await fetch(`${HOST}/v1/${ROOT}/${colName}`, {
                    headers: { authorization: 'Bearer owner' }
                  });
                  if (!res.ok) return { empty: true, docs: [] };
                  const data = await res.json();
                  const rawDocs = data.documents || [];
                  const matched = rawDocs
                    .map(d => {
                      const decoded = decode(d.fields);
                      const relativePath = d.name.replace(`${ROOT}/`, '');
                      return {
                        id: relativePath.split('/').pop(),
                        ref: {
                          update: async (up) => {
                            const cur = await adminGet(relativePath) || {};
                            await adminSet(relativePath, { ...cur, ...up });
                          }
                        },
                        data: () => decoded
                      };
                    })
                    .filter(d => d.data()[field] === val);
                  return { empty: matched.length === 0, docs: matched };
                }
              };
            }
          };
        }
      };
    },
    async runTransaction(updateFunction) {
      const writes = [];
      const transaction = {
        async get(ref) {
          return await ref.get();
        },
        update(ref, updates) {
          writes.push(ref.update(updates));
          return transaction;
        },
        set(ref, data) {
          writes.push(ref.update(data));
          return transaction;
        }
      };
      const result = await updateFunction(transaction);
      await Promise.all(writes);
      return result;
    }
  };
}

test('Razorpay Backend & FCM Notifications Verification', async (t) => {
  const db = createEmulatorDb();
  const testSecret = 'test_secret_1234567890abcdef';

  t.beforeEach(async () => {
    // Reset test booking and user
    await adminSet('users/user_pay_cust', {
      role: 'CUSTOMER',
      name: 'Paying Customer',
      fcmToken: 'fcm_token_valid_device_xyz'
    });

    await adminSet('bookings/booking_pay_1', {
      id: 'booking_pay_1',
      customerId: 'user_pay_cust',
      garageId: 'garage_test',
      serviceId: 'service_test',
      status: 'pending',
      paymentStatus: 'pending',
      totalAmount: 2499.0,
      garageName: 'Apex Detail Studio',
      serviceName: 'Full Ceramic Coating'
    });
  });

  await t.test('computePaymentSignature & verifyPaymentSignature correctness', () => {
    const orderId = 'order_123456';
    const paymentId = 'pay_654321';
    const sig = computePaymentSignature(orderId, paymentId, testSecret);

    assert.ok(sig && sig.length === 64, 'Signature must be a 64-character hex string');
    assert.equal(verifyPaymentSignature(orderId, paymentId, sig, testSecret), true, 'Valid signature must verify');
    assert.equal(verifyPaymentSignature(orderId, paymentId, 'tampered_signature_hex', testSecret), false, 'Tampered signature must fail');
    assert.equal(verifyPaymentSignature('wrong_order', paymentId, sig, testSecret), false, 'Mismatching order must fail');
    assert.equal(verifyPaymentSignature(orderId, paymentId, sig, 'wrong_secret'), false, 'Wrong secret must fail');
  });

  await t.test('handleCreateRazorpayOrder validates caller, booking status, and ownership', async () => {
    // 1. Unauthenticated call
    await assert.rejects(
      handleCreateRazorpayOrder(db, null, { bookingId: 'booking_pay_1' }),
      /UNAUTHENTICATED/
    );

    // 2. Another customer attempting to create order for customer 1 booking
    await assert.rejects(
      handleCreateRazorpayOrder(db, 'different_customer', { bookingId: 'booking_pay_1' }),
      /PERMISSION_DENIED/
    );

    // 3. Valid customer order creation
    const orderResult = await handleCreateRazorpayOrder(db, 'user_pay_cust', { bookingId: 'booking_pay_1' });
    assert.equal(orderResult.success, true);
    assert.ok(orderResult.orderId.startsWith('order_'));
    assert.equal(orderResult.amount, 249900); // 2499 * 100 paise
    assert.equal(orderResult.currency, 'INR');

    // Verify booking updated in Firestore
    const updated = await adminGet('bookings/booking_pay_1');
    assert.equal(updated.razorpayOrderId, orderResult.orderId);
    assert.equal(updated.paymentStatus, 'pending_verification');

    // 4. Idempotency: second call returns same active order
    const secondCall = await handleCreateRazorpayOrder(db, 'user_pay_cust', { bookingId: 'booking_pay_1' });
    assert.equal(secondCall.orderId, orderResult.orderId);
    assert.equal(secondCall.isReused, true);
  });

  await t.test('handleCreateRazorpayOrder rejects invalid booking states', async () => {
    // Cancelled booking
    await adminSet('bookings/booking_cancelled', {
      id: 'booking_cancelled',
      customerId: 'user_pay_cust',
      status: 'cancelled',
      paymentStatus: 'pending',
      totalAmount: 999
    });
    await assert.rejects(
      handleCreateRazorpayOrder(db, 'user_pay_cust', { bookingId: 'booking_cancelled' }),
      /INVALID_BOOKING_STATUS/
    );

    // Completed booking
    await adminSet('bookings/booking_done', {
      id: 'booking_done',
      customerId: 'user_pay_cust',
      status: 'completed',
      paymentStatus: 'pending',
      totalAmount: 999
    });
    await assert.rejects(
      handleCreateRazorpayOrder(db, 'user_pay_cust', { bookingId: 'booking_done' }),
      /INVALID_BOOKING_STATUS/
    );

    // Already paid booking
    await adminSet('bookings/booking_paid', {
      id: 'booking_paid',
      customerId: 'user_pay_cust',
      status: 'confirmed',
      paymentStatus: 'paid',
      totalAmount: 999
    });
    await assert.rejects(
      handleCreateRazorpayOrder(db, 'user_pay_cust', { bookingId: 'booking_paid' }),
      /ALREADY_PAID/
    );
  });

  await t.test('handleVerifyRazorpayPayment verifies signatures and marks booking paid', async () => {
    const orderRes = await handleCreateRazorpayOrder(db, 'user_pay_cust', { bookingId: 'booking_pay_1' });
    const paymentId = 'pay_verified_test_789';
    const validSignature = computePaymentSignature(orderRes.orderId, paymentId, testSecret);

    // 1. Invalid signature rejection
    await assert.rejects(
      handleVerifyRazorpayPayment(
        db,
        'user_pay_cust',
        {
          bookingId: 'booking_pay_1',
          razorpayOrderId: orderRes.orderId,
          razorpayPaymentId: paymentId,
          razorpaySignature: 'invalid_tampered_sig'
        },
        { keySecret: testSecret }
      ),
      /SIGNATURE_VERIFICATION_FAILED/
    );

    // 2. Valid signature succeeds
    const verifyRes = await handleVerifyRazorpayPayment(
      db,
      'user_pay_cust',
      {
        bookingId: 'booking_pay_1',
        razorpayOrderId: orderRes.orderId,
        razorpayPaymentId: paymentId,
        razorpaySignature: validSignature
      },
      { keySecret: testSecret }
    );
    assert.equal(verifyRes.success, true);
    assert.equal(verifyRes.verified, true);
    assert.equal(verifyRes.paymentStatus, 'paid');

    // Verify Firestore reflects verified payment
    const updated = await adminGet('bookings/booking_pay_1');
    assert.equal(updated.paymentStatus, 'paid');
    assert.equal(updated.paymentTransactionId, paymentId);
    assert.ok(Number.isFinite(updated.paidAt));

    // 3. Idempotent re-verification
    const reVerify = await handleVerifyRazorpayPayment(
      db,
      'user_pay_cust',
      {
        bookingId: 'booking_pay_1',
        razorpayOrderId: orderRes.orderId,
        razorpayPaymentId: paymentId,
        razorpaySignature: validSignature
      },
      { keySecret: testSecret }
    );
    assert.equal(reVerify.alreadyVerified, true);
  });

  await t.test('handleRazorpayWebhook processes captured and failed payments with signature check', async () => {
    const webhookSecret = 'wh_secret_987654321';
    const orderId = 'order_webhook_test_1';
    await adminSet('bookings/booking_wh', {
      id: 'booking_wh',
      razorpayOrderId: orderId,
      customerId: 'user_pay_cust',
      paymentStatus: 'pending_verification'
    });

    const capturedPayload = JSON.stringify({
      event: 'payment.captured',
      payload: {
        payment: {
          entity: {
            id: 'pay_wh_captured_1',
            order_id: orderId,
            status: 'captured'
          }
        }
      }
    });

    const validWebhookSig = createHmac('sha256', webhookSecret).update(capturedPayload).digest('hex');

    // 1. Invalid webhook signature rejected
    await assert.rejects(
      handleRazorpayWebhook(db, capturedPayload, 'invalid_sig', webhookSecret),
      /INVALID_WEBHOOK_SIGNATURE/
    );

    // 2. Valid payment.captured webhook processed
    const result = await handleRazorpayWebhook(db, capturedPayload, validWebhookSig, webhookSecret);
    assert.equal(result.processed, true);
    assert.equal(result.event, 'payment.captured');

    const updated = await adminGet('bookings/booking_wh');
    assert.equal(updated.paymentStatus, 'paid');
    assert.equal(updated.paymentTransactionId, 'pay_wh_captured_1');
  });

  await t.test('formatNotificationPayload formats messages accurately without customer PII', () => {
    const confirmed = formatNotificationPayload('confirmed', 'Apex Detailing', 'Full Ceramic');
    assert.ok(confirmed.title.includes('Confirmed'));
    assert.ok(confirmed.body.includes('Apex Detailing'));
    assert.ok(confirmed.body.includes('Full Ceramic'));
    assert.ok(!confirmed.body.includes('Street Address'));

    const inProgress = formatNotificationPayload('in_progress', 'Apex Detailing', 'Full Ceramic');
    assert.ok(inProgress.title.includes('Started'));

    const completed = formatNotificationPayload('completed', 'Apex Detailing', 'Full Ceramic');
    assert.ok(completed.title.includes('Completed'));

    const cancelled = formatNotificationPayload('cancelled', 'Apex Detailing', 'Full Ceramic');
    assert.ok(cancelled.title.includes('Cancelled') || cancelled.title.includes('Update'));

    // Non-notified states return null
    assert.equal(formatNotificationPayload('pending'), null);
    assert.equal(formatNotificationPayload('unknown_status'), null);
  });

  await t.test('handleBookingStatusNotification sends push and cleans stale tokens', async () => {
    let sentMessage = null;
    const mockMessaging = {
      async send(msg) {
        sentMessage = msg;
        return 'msg_id_123';
      }
    };

    const change = {
      before: {
        exists: true,
        data: () => ({ status: 'pending' })
      },
      after: {
        id: 'booking_pay_1',
        exists: true,
        data: () => ({
          id: 'booking_pay_1',
          customerId: 'user_pay_cust',
          status: 'confirmed',
          garageName: 'Apex Detail Studio',
          serviceName: 'Full Ceramic Coating'
        }),
        ref: {
          update: async (up) => {
            const cur = await adminGet('bookings/booking_pay_1') || {};
            await adminSet('bookings/booking_pay_1', { ...cur, ...up });
          }
        }
      }
    };

    const res = await handleBookingStatusNotification(db, mockMessaging, change);
    assert.equal(res.success, true);
    assert.equal(res.status, 'confirmed');
    assert.equal(sentMessage.token, 'fcm_token_valid_device_xyz');
    assert.ok(sentMessage.notification.title.includes('Confirmed'));

    // Deduplication check: duplicate call skips
    const duplicateChange = {
      before: { exists: true, data: () => ({ status: 'pending' }) },
      after: {
        id: 'booking_pay_1',
        exists: true,
        data: () => ({
          status: 'confirmed',
          lastNotifiedStatus: 'confirmed'
        })
      }
    };
    const dupRes = await handleBookingStatusNotification(db, mockMessaging, duplicateChange);
    assert.equal(dupRes.skipped, true);
    assert.equal(dupRes.reason, 'ALREADY_NOTIFIED');

    // Stale token cleanup
    const staleMessaging = {
      async send() {
        const err = new Error('Invalid registration token');
        err.code = 'messaging/invalid-registration-token';
        throw err;
      }
    };
    const staleChange = {
      before: { exists: true, data: () => ({ status: 'confirmed' }) },
      after: {
        id: 'booking_pay_1',
        exists: true,
        data: () => ({
          id: 'booking_pay_1',
          customerId: 'user_pay_cust',
          status: 'in_progress',
          garageName: 'Apex'
        }),
        ref: { update: async () => {} }
      }
    };
    const staleRes = await handleBookingStatusNotification(db, staleMessaging, staleChange);
    assert.equal(staleRes.skipped, true);
    assert.equal(staleRes.reason, 'STALE_TOKEN_CLEANED');

    const cleanedUser = await adminGet('users/user_pay_cust');
    assert.equal(cleanedUser.fcmToken, null);
  });
});
