package com.example.mad_project_akshaysathaye_c049.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Amber colour used consistently for all star icons. */
private val StarAmber = Color(0xFFFFB300)

/**
 * Interactive star-rating input component for the review submission form.
 *
 * Displays [maxStars] star icons. Filled stars are rendered in amber; empty stars
 * are rendered in a muted surface colour. Tapping a star updates [currentRating].
 *
 * @param currentRating  Current integer star selection in [0, maxStars].
 * @param onRatingChange Callback invoked with the newly selected star value.
 * @param maxStars       Total number of stars displayed (default 5).
 * @param starSize       Diameter of each star icon in dp (default 40).
 * @param modifier       Modifier applied to the outer Row.
 */
@Composable
fun StarRatingInput(
    currentRating: Int,
    onRatingChange: (Int) -> Unit,
    maxStars: Int = 5,
    starSize: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val labels = listOf("Terrible", "Poor", "Average", "Good", "Excellent")

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (star in 1..maxStars) {
                val isFilled = star <= currentRating
                Text(
                    text = if (isFilled) "?" else "?",
                    fontSize = starSize.value.sp,
                    color = if (isFilled) StarAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(starSize)
                        .clickable { onRatingChange(star) }
                        .semantics {
                            contentDescription = "Rate $star out of $maxStars stars"
                        }
                )
            }
        }

        if (currentRating in 1..maxStars) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = labels[currentRating - 1],
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = StarAmber
                )
            )
        }
    }
}

/**
 * Read-only compact star rating display reused across list items and cards.
 *
 * Shows a filled amber star followed by the numeric rating and an optional
 * review-count in parentheses.
 */
@Composable
fun StarRatingDisplay(
    rating: Double,
    reviewCount: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "?",
            color = StarAmber,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = " ${String.format("%.1f", rating)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 2.dp)
        )
        if (reviewCount != null) {
            Text(
                text = "  (${reviewCount})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
