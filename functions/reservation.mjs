import { createHash } from 'node:crypto';

const TIME_REGEX = /^(0[1-9]|1[0-2]):[0-5][0-9] (AM|PM)$/;
const DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;
const OFFSET_IST = 19_800_000; // 5.5 hours IST offset

/**
 * Deterministically generates canonical slot ID and timing attributes.
 * Guarantee: Any reservation for the same garage, calendar date, and time slot maps to the exact same slot ID.
 */
export function computeCanonicalSlotId(garageId, dateString, scheduledTime) {
  if (!garageId || typeof garageId !== 'string' || !/^[A-Za-z0-9_-]{1,128}$/.test(garageId)) {
    throw new Error('INVALID_GARAGE_ID');
  }
  if (!dateString || typeof dateString !== 'string' || !DATE_REGEX.test(dateString)) {
    throw new Error('INVALID_DATE_FORMAT');
  }
  if (!scheduledTime || typeof scheduledTime !== 'string' || !TIME_REGEX.test(scheduledTime)) {
    throw new Error('INVALID_TIME_FORMAT');
  }

  const utc = Date.parse(`${dateString}T00:00:00.000Z`);
  if (!Number.isFinite(utc) || new Date(utc).toISOString().slice(0, 10) !== dateString) {
    throw new Error('INVALID_CALENDAR_DATE');
  }

  const canonical = `${garageId.length}:${garageId}:10:${dateString}:${scheduledTime.length}:${scheduledTime}`;
  const hash = createHash('sha256').update(canonical).digest('hex').slice(0, 16);
  const cleanGarage = garageId.replace(/[^a-zA-Z0-9]/g, '_').slice(0, 24);
  const cleanTime = scheduledTime.replace(/[^a-zA-Z0-9]/g, '');
  const id = `slot_${cleanGarage}_${dateString}_${cleanTime}_${hash}`;

  return {
    id,
    garageId,
    dateString,
    scheduledTime,
    scheduledDate: utc - OFFSET_IST,
    timeZoneId: 'Asia/Kolkata',
    reservationVersion: 2
  };
}

/**
 * Atomic Slot Reservation Handler.
 * Runs in a Firestore transaction to guarantee that two concurrent customers
 * can NEVER reserve the same workshop slot.
 *
 * Enforces:
 *  1. Caller authentication and customer ownership.
 *  2. Verification that customer owns the specified vehicle profile.
 *  3. Verification that garage and detailing service exist.
 *  4. Optimistic concurrency control across the canonical slot document.
 */
export async function handleSlotReservation(db, callerUid, payload) {
  if (!callerUid || typeof callerUid !== 'string') {
    throw new Error('UNAUTHENTICATED: Caller must be signed in to reserve a slot.');
  }

  const {
    customerId,
    garageId,
    serviceId,
    vehicleId,
    dateString,
    scheduledTime,
    customerNotes = '',
    paymentMethod = 'pay_at_garage'
  } = payload;

  if (customerId !== callerUid) {
    throw new Error('PERMISSION_DENIED: Cannot create a booking on behalf of another user.');
  }

  const slotIdentity = computeCanonicalSlotId(garageId, dateString, scheduledTime);
  const slotRef = db.doc(`slots/${slotIdentity.id}`);
  const garageRef = db.doc(`garages/${garageId}`);
  const serviceRef = db.doc(`services/${serviceId}`);
  const vehicleRef = db.doc(`vehicles/${vehicleId}`);
  const bookingId = payload.bookingId || `booking_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const bookingRef = db.doc(`bookings/${bookingId}`);

  return await db.runTransaction(async (transaction) => {
    // 1. Transactional Reads
    const [slotDoc, garageDoc, serviceDoc, vehicleDoc] = await Promise.all([
      transaction.get(slotRef),
      transaction.get(garageRef),
      transaction.get(serviceRef),
      transaction.get(vehicleRef)
    ]);

    // 2. Precondition Validations
    if (!garageDoc.exists) {
      throw new Error(`GARAGE_NOT_FOUND: Workshop ${garageId} does not exist.`);
    }
    if (!serviceDoc.exists) {
      throw new Error(`SERVICE_NOT_FOUND: Service ${serviceId} does not exist.`);
    }
    const serviceData = serviceDoc.data();
    if (serviceData.garageId && serviceData.garageId !== garageId) {
      throw new Error('SERVICE_MISMATCH: Detailing service does not belong to the selected garage.');
    }

    if (!vehicleDoc.exists) {
      throw new Error(`VEHICLE_NOT_FOUND: Vehicle ${vehicleId} does not exist.`);
    }
    const vehicleData = vehicleDoc.data();
    if (vehicleData.ownerId !== callerUid) {
      throw new Error('UNAUTHORIZED_VEHICLE: Selected vehicle does not belong to your account.');
    }

    // 3. Slot Conflict Validation
    if (slotDoc.exists) {
      const slotData = slotDoc.data();
      const isReserved = slotData.isReserved === true || slotData.reserved === true;
      const isActiveStatus = slotData.status !== 'cancelled' && slotData.status !== 'available';
      if (isReserved && isActiveStatus) {
        throw new Error('SLOT_ALREADY_RESERVED: This time slot is already booked. Please choose another slot.');
      }
    }

    const garageData = garageDoc.data();
    const now = Date.now();

    // 4. Atomic Writes
    const newSlotData = {
      id: slotIdentity.id,
      garageId,
      dateString,
      scheduledTime,
      scheduledDate: slotIdentity.scheduledDate,
      timeZoneId: slotIdentity.timeZoneId,
      reservationVersion: 2,
      isReserved: true,
      customerId: callerUid,
      bookingId,
      status: 'reserved',
      updatedAt: now
    };

    const newBookingData = {
      id: bookingId,
      customerId: callerUid,
      garageId,
      serviceId,
      vehicleId,
      slotId: slotIdentity.id,
      dateString,
      scheduledTime,
      scheduledDate: slotIdentity.scheduledDate,
      timeZoneId: slotIdentity.timeZoneId,
      status: 'pending',
      totalAmount: Number(serviceData.price) || 0.0,
      notes: customerNotes.trim(),
      garageName: garageData.name || 'AutoCare Workshop',
      garageAddress: `${garageData.address || ''}, ${garageData.city || ''}`.trim(),
      serviceName: serviceData.name || 'Detailing Service',
      vehicleDetails: `${vehicleData.make || ''} ${vehicleData.model || ''} (${vehicleData.licensePlate || ''})`.trim(),
      paymentMethod,
      paymentStatus: paymentMethod === 'online_test' ? 'pending_verification' : 'pending',
      createdAt: now,
      updatedAt: now
    };

    transaction.set(slotRef, newSlotData);
    transaction.set(bookingRef, newBookingData);

    return {
      success: true,
      bookingId,
      slotId: slotIdentity.id,
      scheduledDate: slotIdentity.scheduledDate,
      scheduledTime
    };
  });
}

/**
 * Atomic Slot Cancellation Handler.
 * Frees the associated slot when a booking is cancelled.
 */
export async function handleSlotCancellation(db, callerUid, bookingId, cancellationReason = '') {
  if (!callerUid || typeof callerUid !== 'string') {
    throw new Error('UNAUTHENTICATED: Caller must be signed in to cancel a booking.');
  }
  if (!bookingId || typeof bookingId !== 'string') {
    throw new Error('INVALID_BOOKING_ID: A valid booking ID is required.');
  }

  const bookingRef = db.doc(`bookings/${bookingId}`);

  return await db.runTransaction(async (transaction) => {
    const bookingDoc = await transaction.get(bookingRef);
    if (!bookingDoc.exists) {
      throw new Error(`BOOKING_NOT_FOUND: Booking ${bookingId} not found.`);
    }

    const bookingData = bookingDoc.data();

    // Verify ownership: customer who booked or garage owner of the workshop
    let isAuthorized = bookingData.customerId === callerUid;
    if (!isAuthorized && bookingData.garageId) {
      const garageDoc = await transaction.get(db.doc(`garages/${bookingData.garageId}`));
      if (garageDoc.exists && garageDoc.data().ownerId === callerUid) {
        isAuthorized = true;
      }
    }

    if (!isAuthorized) {
      throw new Error('PERMISSION_DENIED: You do not have permission to cancel this booking.');
    }

    if (bookingData.status === 'completed') {
      throw new Error('ILLEGAL_STATE: Cannot cancel an already completed service.');
    }
    if (bookingData.status === 'cancelled') {
      throw new Error('ILLEGAL_STATE: Booking is already cancelled.');
    }

    const now = Date.now();
    transaction.update(bookingRef, {
      status: 'cancelled',
      cancellationReason: cancellationReason.trim(),
      updatedAt: now
    });

    // Release the slot if one was linked
    if (bookingData.slotId) {
      const slotRef = db.doc(`slots/${bookingData.slotId}`);
      const slotDoc = await transaction.get(slotRef);
      if (slotDoc.exists) {
        transaction.update(slotRef, {
          isReserved: false,
          status: 'available',
          bookingId: null,
          updatedAt: now
        });
      }
    }

    return {
      success: true,
      bookingId,
      status: 'cancelled'
    };
  });
}
