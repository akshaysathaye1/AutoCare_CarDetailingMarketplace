package com.example.mad_project_akshaysathaye_c049.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mad_project_akshaysathaye_c049.ui.components.AppCard
import com.example.mad_project_akshaysathaye_c049.ui.components.AutoCareTopBar
import com.example.mad_project_akshaysathaye_c049.ui.components.GarageBottomBar
import com.example.mad_project_akshaysathaye_c049.ui.components.RatingComponent
import com.example.mad_project_akshaysathaye_c049.ui.components.SectionHeading

@Composable
fun GarageProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "Workshop Profile",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            GarageBottomBar(
                currentRoute = "garage_profile",
                onNavigateToRoute = onNavigateToRoute
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🔧", fontSize = 32.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Apex Motor Works",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Authorized Detailing & Service Partner",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    RatingComponent(rating = 4.8, reviewCount = 184)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contact & Location Card
            SectionHeading(title = "Location & Contact Information")
            Spacer(modifier = Modifier.height(10.dp))

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row {
                        Text(text = "📍 ", fontSize = 18.sp)
                        Column {
                            Text(
                                text = "Address",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Plot 42, Electronic City Phase 1, Hosur Road, Bangalore, Karnataka 560100",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        Text(text = "📞 ", fontSize = 18.sp)
                        Column {
                            Text(
                                text = "Phone Number",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "+91 98450 12345 / +91 80 2852 9999",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        Text(text = "✉️ ", fontSize = 18.sp)
                        Column {
                            Text(
                                text = "Email Address",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "support@apexmotorworks.in",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Operating Hours
            SectionHeading(title = "Operating Timings")
            Spacer(modifier = Modifier.height(10.dp))

            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Monday – Saturday", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "8:30 AM – 8:00 PM",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Sunday", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "9:00 AM – 3:00 PM",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Description & Facilities
            SectionHeading(title = "About Workshop")
            Spacer(modifier = Modifier.height(10.dp))

            AppCard {
                Text(
                    text = "Apex Motor Works is a multi-brand automotive detailing studio and mechanical service center. Equipped with 6 dedicated detailing bays, dust-free paint protection film booth, automated underbody wash lift, and trained technicians.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
