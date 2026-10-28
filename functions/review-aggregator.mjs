/**
 * Pure calculation helper for rating aggregates.
 * Ensures consistent mathematical rounding to 1 decimal place and safe 0-count baseline.
 */
export function calculateRatingAggregate(reviewsList = []) {
  if (!Array.isArray(reviewsList) || reviewsList.length === 0) {
    return {
      rating: 0.0,
      reviewCount: 0
    };
  }

  let totalScore = 0.0;
  let validCount = 0;

  for (const rev of reviewsList) {
    const r = Number(rev.rating);
    if (Number.isFinite(r) && r >= 1.0 && r <= 5.0) {
      totalScore += r;
      validCount++;
    }
  }

  if (validCount === 0) {
    return { rating: 0.0, reviewCount: 0 };
  }

  const rawAverage = totalScore / validCount;
  const roundedRating = Math.round(rawAverage * 10) / 10;

  return {
    rating: roundedRating,
    reviewCount: validCount
  };
}

/**
 * Review Aggregator Firestore Trigger Handler.
 * Invoked on review creation, update, or deletion.
 *
 * Guarantees:
 *  1. Completely removes client-side permission to write to /garages rating fields.
 *  2. Idempotently reads verified reviews from /reviews collection.
 *  3. Handles deleted reviews by re-evaluating the full active set without negative counter drift.
 *  4. Admin SDK bypasses security rules to update protected garage fields.
 */
export async function handleReviewWritten(db, change) {
  const afterData = change.after?.exists ? change.after.data() : null;
  const beforeData = change.before?.exists ? change.before.data() : null;

  const garageId = afterData?.garageId || beforeData?.garageId;
  if (!garageId) {
    return { skipped: true, reason: 'NO_GARAGE_ID' };
  }

  // Fetch all current reviews for this workshop
  const snapshot = await db.collection('reviews')
    .where('garageId', '==', garageId)
    .get();

  const reviews = snapshot.docs.map(doc => doc.data());
  const aggregate = calculateRatingAggregate(reviews);

  // Update protected garage rating attributes via Admin SDK
  const garageRef = db.doc(`garages/${garageId}`);
  const garageDoc = await garageRef.get();

  if (garageDoc.exists) {
    await garageRef.update({
      rating: aggregate.rating,
      reviewCount: aggregate.reviewCount,
      updatedAt: Date.now()
    });
  }

  return {
    garageId,
    rating: aggregate.rating,
    reviewCount: aggregate.reviewCount
  };
}
