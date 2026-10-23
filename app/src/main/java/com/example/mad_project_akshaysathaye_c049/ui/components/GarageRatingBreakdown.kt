package com.example.mad_project_akshaysathaye_c049.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mad_project_akshaysathaye_c049.data.model.Review

private val StarAmberColor = Color(0xFFFFB300)

/**
 * GarageRatingBreakdown displays a full rating summary widget showing:
 *  - Average rating as a large number
 *  - Star distribution bars (5-star down to 1-star)
 *  - Total review count
 *
 * All values are calculated from the [reviews] list at the call site;
 * no data is hardcoded. Used on the Garage Dashboard and the Reviews tab.
 *
 * @param reviews       List of Review objects from Firestore.
 * @param modifier      Composable modifier.
 */
@Composable
fun GarageRatingBreakdown(
    reviews: List<Review>,
    modifier: Modifier = Modifier
) {
    val totalCount = reviews.size
    val average = if (totalCount == 0) 0.0 else reviews.sumOf { it.rating.toDouble() } / totalCount
    val rounded = Math.round(average * 10.0) / 10.0

    // Count reviews per star tier (5 down to 1)
    val starCounts = (5 downTo 1).map { star ->
        star to reviews.count { it.rating.toInt() == star || (star == 1 && it.rating < 1.5f) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Big average number + stars
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format("%.1f", rounded),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = StarAmberColor
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row {
                    repeat(5) { idx ->
                        Text(
                            text = if (idx < rounded.toInt()) "?" else "?",
                            color = StarAmberColor,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Text(
                    text = "$totalCount ${if (totalCount == 1) "review" else "reviews"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Star distribution bars
        starCounts.forEach { (star, count) ->
            val fraction = if (totalCount == 0) 0f else count.toFloat() / totalCount
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$star ?",
                    style = MaterialTheme.typography.labelMedium,
                    color = StarAmberColor,
                    modifier = Modifier.width(36.dp)
                )
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    color = StarAmberColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(24.dp)
                )
            }
        }
    }
}
