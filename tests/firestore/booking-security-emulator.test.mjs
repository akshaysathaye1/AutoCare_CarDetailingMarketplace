import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { HOST, ROOT } from '../../tools/reservation-migration/emulator.mjs';
import { fields, decode } from '../../tools/reservation-migration/plan.mjs';
import { computeCanonicalSlotId } from '../../functions/reservation.mjs';
import { calculateRatingAggregate } from '../../functions/review-aggregator.mjs';

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

async function clientGet(path, uid = 'customer_1') {
  return fetch(`${HOST}/v1/${ROOT}/${path}`, {
    method: 'GET',
    redirect: 'error',
    headers: {
      authorization: `Bearer ${token(uid)}`,
      'content-type': 'application/json'
    }
  });
}

async function clientWrite(path, data, uid = 'customer_1') {
  return fetch(`${HOST}/v1/${ROOT}:commit`, {
    method: 'POST',
    redirect: 'error',
    headers: {
      authorization: `Bearer ${token(uid)}`,
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

async function clientDelete(path, uid = 'customer_1') {
  return fetch(`${HOST}/v1/${ROOT}:commit`, {
    method: 'POST',
    redirect: 'error',
    headers: {
      authorization: `Bearer ${token(uid)}`,
      'content-type': 'application/json'
    },
    body: JSON.stringify({
      writes: [{
        delete: `${ROOT}/${path}`
      }]
    })
  });
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

async function resetDatabase() {
  const response = await fetch(`${HOST}/emulator/v1/projects/demo-autocare-migration/databases/(default)/documents`, {
    method: 'DELETE',
    redirect: 'error'
  });
  assert.ok(response.ok, 'Resetting local emulator database must succeed');
}

test('Firestore Security & Concurrency Verification', async (t) => {
  // 1. Push latest firestore.rules to emulator
  const rulesContent = readFileSync(new URL('../../firestore.rules', import.meta.url), 'utf8');
  const rulesRes = await fetch(`${HOST}/emulator/v1/projects/demo-autocare-migration:securityRules`, {
    method: 'PUT',
    redirect: 'error',
    headers: {
      authorization: 'Bearer owner',
      'content-type': 'application/json'
    },
    body: JSON.stringify({
      rules: {
        files: [{ name: 'firestore.rules', content: rulesContent }]
      }
    })
  });
  assert.ok(rulesRes.ok, 'Firestore security rules must compile and load successfully into emulator');

  t.beforeEach(async () => {
    await resetDatabase();

    // Seed baseline entities via Admin SDK
    await adminSet('users/customer_1', { role: 'CUSTOMER', name: 'Customer One' });
    await adminSet('users/customer_2', { role: 'CUSTOMER', name: 'Customer Two' });
    await adminSet('users/garage_owner', { role: 'GARAGE', name: 'Apex Garage Owner' });
    await adminSet('users/garage_owner_2', { role: 'GARAGE', name: 'Rival Garage Owner' });
    await adminSet('garages/garage_test', {
      id: 'garage_test',
      name: 'Apex Detail Studio',
      ownerId: 'garage_owner',
      rating: 4.8,
      reviewCount: 25,
      address: '123 Detailing Way',
      city: 'Mumbai'
    });
    await adminSet('garages/garage_test_2', {
      id: 'garage_test_2',
      name: 'Rival Detail Studio',
      ownerId: 'garage_owner_2',
      rating: 4.0,
      reviewCount: 5,
      address: '456 Rival Way',
      city: 'Mumbai'
    });
    await adminSet('services/service_test', {
      id: 'service_test',
      garageId: 'garage_test',
      name: 'Full Body Ceramic Coating',
      price: 14999
    });
    await adminSet('vehicles/vehicle_cust1', {
      id: 'vehicle_cust1',
      ownerId: 'customer_1',
      make: 'Honda',
      model: 'City'
    });
    await adminSet('vehicles/vehicle_cust2', {
      id: 'vehicle_cust2',
      ownerId: 'customer_2',
      make: 'Hyundai',
      model: 'Creta'
    });
  });

  await t.test('Client cannot modify protected garage rating and reviewCount fields', async () => {
    // Attempt 1: Customer tries to modify garage rating directly
    const spoofRatingRes = await clientWrite('garages/garage_test', { rating: 1.0 }, 'customer_1');
    assert.equal(spoofRatingRes.status, 403, 'Customer write to garage rating must be rejected with 403 Forbidden');

    // Attempt 2: Garage owner tries to inflate own rating directly
    const spoofOwnerRatingRes = await clientWrite('garages/garage_test', { rating: 5.0, reviewCount: 1000 }, 'garage_owner');
    assert.equal(spoofOwnerRatingRes.status, 403, 'Garage owner write to rating/reviewCount must be rejected with 403 Forbidden');

    // Attempt 3: Garage owner updates other non-rating fields keeping rating and reviewCount unchanged
    const legitimateUpdateRes = await clientWrite('garages/garage_test', {
      name: 'Apex Detail Studio & PPF',
      rating: 4.8,
      reviewCount: 25
    }, 'garage_owner');
    assert.equal(legitimateUpdateRes.status, 200, 'Garage owner update preserving rating/reviewCount must succeed');
  });

  await t.test('Private customer booking documents cannot be read by another customer', async () => {
    // Customer 1 creates a private booking
    const createBookingRes = await clientWrite('bookings/booking_c1', {
      id: 'booking_c1',
      customerId: 'customer_1',
      garageId: 'garage_test',
      serviceId: 'service_test',
      vehicleId: 'vehicle_cust1',
      status: 'pending',
      scheduledDate: Date.now(),
      scheduledTime: '10:00 AM'
    }, 'customer_1');
    assert.equal(createBookingRes.status, 200, 'Customer 1 can create their own booking');

    // Customer 2 attempts to read Customer 1 booking
    const snoopingRes = await clientGet('bookings/booking_c1', 'customer_2');
    assert.equal(snoopingRes.status, 403, 'Customer 2 reading Customer 1 booking must be blocked with 403 Forbidden');

    // Customer 1 can read their own booking
    const legitimateReadRes = await clientGet('bookings/booking_c1', 'customer_1');
    assert.equal(legitimateReadRes.status, 200, 'Customer 1 can read their own booking');
  });

  await t.test('Atomic slot collision: Second customer is blocked from reserving already reserved slot', async () => {
    const slotIdentity = computeCanonicalSlotId('garage_test', '2030-05-15', '11:00 AM');
    const slotDocPath = `slots/${slotIdentity.id}`;

    // Customer 1 reserves slot
    const c1ReservationRes = await clientWrite(slotDocPath, {
      id: slotIdentity.id,
      garageId: 'garage_test',
      dateString: '2030-05-15',
      scheduledTime: '11:00 AM',
      customerId: 'customer_1',
      bookingId: 'booking_c1',
      isReserved: true,
      status: 'reserved'
    }, 'customer_1');
    assert.equal(c1ReservationRes.status, 200, 'Customer 1 can reserve available slot');

    // Customer 2 attempts to overwrite or claim the same slot
    const c2CollisionRes = await clientWrite(slotDocPath, {
      id: slotIdentity.id,
      garageId: 'garage_test',
      dateString: '2030-05-15',
      scheduledTime: '11:00 AM',
      customerId: 'customer_2',
      bookingId: 'booking_c2',
      isReserved: true,
      status: 'reserved'
    }, 'customer_2');
    assert.equal(c2CollisionRes.status, 403, 'Customer 2 must be blocked from acquiring Customer 1 reserved slot (403 Forbidden)');

    // Customer 2 CAN read the slot availability (public schedule coordination, non-PII)
    const availabilityCheck = await clientGet(slotDocPath, 'customer_2');
    assert.equal(availabilityCheck.status, 200, 'Customer 2 can read slot availability for conflict checking without PERMISSION_DENIED');
  });

  await t.test('Released/cancelled slots can be safely re-reserved by another customer', async () => {
    const slotIdentity = computeCanonicalSlotId('garage_test', '2030-06-20', '02:00 PM');
    const slotDocPath = `slots/${slotIdentity.id}`;

    // Customer 1 reserves slot
    await clientWrite(slotDocPath, {
      id: slotIdentity.id,
      garageId: 'garage_test',
      dateString: '2030-06-20',
      scheduledTime: '02:00 PM',
      customerId: 'customer_1',
      bookingId: 'booking_c1',
      isReserved: true,
      status: 'reserved'
    }, 'customer_1');

    // Customer 1 cancels booking and releases slot
    const cancelRes = await clientWrite(slotDocPath, {
      id: slotIdentity.id,
      garageId: 'garage_test',
      dateString: '2030-06-20',
      scheduledTime: '02:00 PM',
      customerId: 'customer_1',
      isReserved: false,
      status: 'available',
      bookingId: ''
    }, 'customer_1');
    assert.equal(cancelRes.status, 200, 'Customer 1 can release their reserved slot');

    // Customer 2 can now acquire the released slot
    const c2AcquireRes = await clientWrite(slotDocPath, {
      id: slotIdentity.id,
      garageId: 'garage_test',
      dateString: '2030-06-20',
      scheduledTime: '02:00 PM',
      customerId: 'customer_2',
      bookingId: 'booking_c2',
      isReserved: true,
      status: 'reserved'
    }, 'customer_2');
    assert.equal(c2AcquireRes.status, 200, 'Customer 2 can acquire released slot');
  });

  await t.test('Review creation and deletion authorizations', async () => {
    // Customer 1 submits review
    const reviewRes = await clientWrite('reviews/review_1', {
      id: 'review_1',
      customerId: 'customer_1',
      garageId: 'garage_test',
      bookingId: 'booking_c1',
      rating: 5.0,
      comment: 'Outstanding ceramic coating finish!'
    }, 'customer_1');
    assert.equal(reviewRes.status, 200, 'Customer 1 can create their review');

    // Customer 2 cannot delete Customer 1 review
    const unauthorizedDelete = await clientDelete('reviews/review_1', 'customer_2');
    assert.equal(unauthorizedDelete.status, 403, 'Customer 2 cannot delete Customer 1 review');

    // Customer 1 can delete their own review
    const authorizedDelete = await clientDelete('reviews/review_1', 'customer_1');
    assert.equal(authorizedDelete.status, 200, 'Customer 1 can delete their own review');
  });

  await t.test('Garage owners cannot modify other garages, bookings, or customer reviews', async () => {
    // 1. Garage Owner 2 attempts to modify Garage 1's profile
    const rivalProfileEdit = await clientWrite('garages/garage_test', {
      name: 'Sabotaged Garage Name',
      rating: 4.8,
      reviewCount: 25,
      ownerId: 'garage_owner'
    }, 'garage_owner_2');
    assert.equal(rivalProfileEdit.status, 403, 'Garage Owner 2 must be blocked from modifying Garage 1 profile');

    // 2. Garage Owner 1 CAN legitimately update their own garage
    const ownProfileEdit = await clientWrite('garages/garage_test', {
      name: 'Apex Detail Studio (Updated)',
      rating: 4.8,
      reviewCount: 25,
      ownerId: 'garage_owner'
    }, 'garage_owner');
    assert.equal(ownProfileEdit.status, 200, 'Garage Owner 1 must be authorized to update their own garage profile');

    // 3. Customer 1 creates a booking for Garage 1
    await clientWrite('bookings/booking_garage1', {
      id: 'booking_garage1',
      customerId: 'customer_1',
      garageId: 'garage_test',
      status: 'pending'
    }, 'customer_1');

    // 4. Garage Owner 2 attempts to update / cancel Garage 1's booking
    const rivalBookingEdit = await clientWrite('bookings/booking_garage1', {
      status: 'cancelled',
      cancellationReason: 'Sabotaged by rival workshop'
    }, 'garage_owner_2');
    assert.equal(rivalBookingEdit.status, 403, 'Garage Owner 2 must be blocked from modifying Garage 1 bookings');

    // 5. Garage Owner 1 CAN update their own garage's booking
    const ownBookingEdit = await clientWrite('bookings/booking_garage1', {
      status: 'confirmed'
    }, 'garage_owner');
    assert.equal(ownBookingEdit.status, 200, 'Garage Owner 1 must be authorized to update their own garage bookings');

    // 6. Garage Owner 1 attempts to delete a customer review
    await clientWrite('reviews/review_to_tamper', {
      id: 'review_to_tamper',
      customerId: 'customer_1',
      garageId: 'garage_test',
      bookingId: 'booking_garage1',
      rating: 2.0,
      comment: 'Average service, car was delivered late.'
    }, 'customer_1');

    const ownerReviewTamper = await clientDelete('reviews/review_to_tamper', 'garage_owner');
    assert.equal(ownerReviewTamper.status, 403, 'Garage owners must be strictly blocked from deleting negative customer reviews');
  });

  await t.test('Trusted Backend: calculateRatingAggregate accuracy & edge cases', () => {
    assert.deepEqual(calculateRatingAggregate([]), { rating: 0.0, reviewCount: 0 });

    const singleReview = [{ rating: 5.0 }];
    assert.deepEqual(calculateRatingAggregate(singleReview), { rating: 5.0, reviewCount: 1 });

    const multiReviews = [
      { rating: 5.0 },
      { rating: 4.0 },
      { rating: 5.0 },
      { rating: 5.0 }
    ];
    // (5 + 4 + 5 + 5) / 4 = 19 / 4 = 4.75 -> rounded to 1 decimal: 4.8
    assert.deepEqual(calculateRatingAggregate(multiReviews), { rating: 4.8, reviewCount: 4 });

    // Excludes invalid ratings
    const withInvalid = [
      { rating: 5.0 },
      { rating: 0.0 }, // invalid
      { rating: 6.0 }, // invalid
      { rating: 3.0 }
    ];
    // (5 + 3) / 2 = 4.0
    assert.deepEqual(calculateRatingAggregate(withInvalid), { rating: 4.0, reviewCount: 2 });
  });

  await t.test('Trusted Backend: computeCanonicalSlotId determinism and security validation', () => {
    const slotA = computeCanonicalSlotId('garage_test', '2030-08-10', '10:00 AM');
    const slotB = computeCanonicalSlotId('garage_test', '2030-08-10', '10:00 AM');
    assert.equal(slotA.id, slotB.id, 'Same parameters must produce identical slot ID');

    const slotDifferentTime = computeCanonicalSlotId('garage_test', '2030-08-10', '11:00 AM');
    assert.notEqual(slotA.id, slotDifferentTime.id, 'Different time slot must produce different slot ID');

    const slotDifferentDate = computeCanonicalSlotId('garage_test', '2030-08-11', '10:00 AM');
    assert.notEqual(slotA.id, slotDifferentDate.id, 'Different date must produce different slot ID');

    assert.throws(() => computeCanonicalSlotId('', '2030-08-10', '10:00 AM'), /INVALID_GARAGE_ID/);
    assert.throws(() => computeCanonicalSlotId('garage_test', 'invalid-date', '10:00 AM'), /INVALID_DATE_FORMAT/);
    assert.throws(() => computeCanonicalSlotId('garage_test', '2030-08-10', '25:00 PM'), /INVALID_TIME_FORMAT/);
  });
});
