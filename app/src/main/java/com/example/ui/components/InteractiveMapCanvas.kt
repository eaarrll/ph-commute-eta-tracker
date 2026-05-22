package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Checkpoint
import com.example.data.models.CheckInReport

@Composable
fun InteractiveMapCanvas(
    checkpoints: List<Checkpoint>,
    reports: List<CheckInReport>,
    waitingCheckpointId: String,
    selectedDirection: String,
    onCheckpointSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (checkpoints.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(260.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Seeding route map...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // Colors
    val roadColor = Color(0xFF2C2C2C)
    val markerColor = MaterialTheme.colorScheme.primary
    val waitingColor = Color(0xFFFF5722) // High contrast orange
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val directionAngle = if (selectedDirection == "Northbound") 0f else 180f

    // Standard high-fidelity Philippine transportation visual container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFF1E293B), RoundedCornerShape(16.dp)) // Cool Slate Dark canvas
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Vertical Road Layout
        Row(modifier = Modifier.fillMaxSize()) {
            
            // Side panel with guide details
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ROUTE MAP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8), // Neon sky blue
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (selectedDirection == "Northbound") "PA-UP (North)" else "PA-LRT (South)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(waitingColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sawsaw (You 📍)", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF10B981), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Active jeep/bus", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF64748B), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bus Stop", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                }
            }

            // Interactive Map Visuals Box
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp)) // Deep space blue
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            ) {
                // Background road grids or dashed line for road lane
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Road centerline (vertical)
                    val roadX = w * 0.45f
                    
                    // Draw outer road borders
                    drawLine(
                        color = Color(0xFF334155),
                        start = Offset(roadX - 25f, 0f),
                        end = Offset(roadX - 25f, h),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF334155),
                        start = Offset(roadX + 25f, 0f),
                        end = Offset(roadX + 25f, h),
                        strokeWidth = 2f
                    )

                    // Draw solid grey asphalt path
                    drawRect(
                        color = roadColor,
                        topLeft = Offset(roadX - 24f, 0f),
                        size = androidx.compose.ui.geometry.Size(48f, h)
                    )

                    // Dashed yellow centerline representing Philippine lanes
                    drawLine(
                        color = Color(0xFFFBBF24),
                        start = Offset(roadX, 0f),
                        end = Offset(roadX, h),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                }

                // Render Checkpoints along the road
                val cpCount = checkpoints.size
                checkpoints.forEachIndexed { index, cp ->
                    // Calculate relative height along path
                    val relativeYPercent = index.toFloat() / (cpCount - 1).coerceAtLeast(1).toFloat()
                    
                    // Southbound starts from UP Town (bottom in vertical list or top? Let's make top index 0 always as start)
                    // The path coordinates:
                    val yOffsetDp = (24 + (230 * relativeYPercent)).dp
                    val isUserWaiting = cp.id == waitingCheckpointId

                    // Align each node on the road centerline
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 10.dp, y = yOffsetDp)
                            .padding(start = 25.dp) // Offset to clear road center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onCheckpointSelected(cp.id) }
                                .background(
                                    if (isUserWaiting) waitingColor.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(2.dp)
                        ) {
                            Text(
                                text = cp.name,
                                color = if (isUserWaiting) waitingColor else Color(0xFFF1F5F9),
                                fontSize = if (isUserWaiting) 10.sp else 9.sp,
                                fontWeight = if (isUserWaiting) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.widthIn(max = 110.dp)
                            )
                        }
                    }

                    // Draw actual checkpoint node on the road itself
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(y = yOffsetDp + 3.dp)
                            .fillMaxWidth()
                    ) {
                        // Node center is at x = 45% of width
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth(0.53f) // Position dot around the center of the road
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.TopEnd)
                                    .background(
                                        if (isUserWaiting) waitingColor else Color(0xFF475569),
                                        CircleShape
                                    )
                                    .border(
                                        2.dp,
                                        if (isUserWaiting) Color.White else Color(0xFF94A3B8),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }

                // Draw Active Reported Vehicles
                // Draw vehicles reported in the last 4 minutes (240000ms)
                val activeReports = reports.filter { System.currentTimeMillis() - it.timestamp < 240000 }
                
                activeReports.forEach { report ->
                    val reportCp = checkpoints.find { it.id == report.checkpointId }
                    if (reportCp != null) {
                        val index = checkpoints.indexOf(reportCp)
                        val relativeYPercent = index.toFloat() / (cpCount - 1).coerceAtLeast(1).toFloat()
                        val yOffsetDp = (20 + (230 * relativeYPercent)).dp

                        // Vehicle Icon based on state
                        val vehicleEmoji = when (report.vehicleType) {
                            "Traditional Jeep" -> "🛺"
                            "Modern Jeep" -> "🚌"
                            "UV Express" -> "🚐"
                            "Bus" -> "🚍"
                            else -> "🚗"
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(y = yOffsetDp)
                                .fillMaxWidth(0.55f) // Right over the road center line
                        ) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color(0xFF10B981), RoundedCornerShape(8.dp)) // Vibrant Green bubble
                                    .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .shadow(2.dp, RoundedCornerShape(8.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(vehicleEmoji, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (report.reporterName.contains("Simulated") || report.reporterName.contains("Manong") || report.reporterName.contains("AteneoStaff")) "LIVE" else "COMMUNITY",
                                    color = Color.White,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Downstream direction arrow pulsating
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulsateY by infiniteTransition.animateFloat(
                    initialValue = -10f,
                    targetValue = 10f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "yOffset"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 12.dp, end = 12.dp)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Direction",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(directionAngle)
                            .offset(y = pulsateY.dp)
                    )
                }
            }
        }
    }
}
