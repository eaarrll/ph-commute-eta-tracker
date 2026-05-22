package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.gemini.GeminiService
import com.example.ui.components.InteractiveMapCanvas

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainCommuterScreen(
    viewModel: CommuterViewModel,
    modifier: Modifier = Modifier
) {
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val checkpoints by viewModel.checkpoints.collectAsStateWithLifecycle()
    val recentReports by viewModel.recentReports.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val etaState by viewModel.etaPrediction.collectAsStateWithLifecycle()

    val selectedRouteId by viewModel.selectedRouteId.collectAsStateWithLifecycle()
    val selectedDirection by viewModel.selectedDirection.collectAsStateWithLifecycle()
    val waitingCheckpointId by viewModel.waitingCheckpointId.collectAsStateWithLifecycle()

    val isSimulatorRunning by viewModel.isSimulatorRunning.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()

    val formVehicleType by viewModel.formVehicleType.collectAsStateWithLifecycle()
    val formCheckpointId by viewModel.formCheckpointId.collectAsStateWithLifecycle()
    val formCrowdStatus by viewModel.formCrowdStatus.collectAsStateWithLifecycle()
    val formReporterStatus by viewModel.formReporterStatus.collectAsStateWithLifecycle()
    val formReporterName by viewModel.formReporterName.collectAsStateWithLifecycle()

    var chatMessageText by remember { mutableStateOf("") }
    var aiQuestionText by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0 = ChatFeed, 1 = Ask AI

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val currentRoute = routes.find { it.id == selectedRouteId }
    val currentWaitingCp = checkpoints.find { it.id == waitingCheckpointId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Para Po!",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "🇵🇭",
                                fontSize = 20.sp
                            )
                        }
                        Text(
                            "Crowdsourced GPS-less Commute ETA",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSimulatorRunning) Color(0xFF10B981).copy(alpha = 0.15f)
                                else Color.Gray.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (isSimulatorRunning) Color(0xFF10B981) else Color.Gray,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isSimulatorRunning) Color(0xFF10B981) else Color.Gray,
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                            Text(
                                text = if (isSimulatorRunning) "SIM ACTIVE" else "LIVE ONLY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSimulatorRunning) Color(0xFF10B981) else Color.Gray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // Introduction & Quick Explanation Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = "Philippine Jeepney icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Welcome to the Pilot Corridor!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Commuters mark 'I'm here' or 'Jeep passed' as they travel. We interpolate and forecast downstream arrival times for you mathematically below.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Top Action Row - Choose Route & Direction
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "1. Choose Corridor & Direction",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Route selection buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        routes.forEach { route ->
                            val isSelected = route.id == selectedRouteId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        viewModel.selectedRouteId.value = route.id
                                        // Auto update form checkpoint default setting
                                        if (route.id == "katipunan_cubao") {
                                            viewModel.waitingCheckpointId.value = "kc_uptc"
                                            viewModel.formCheckpointId.value = "kc_ateneo"
                                        } else if (route.id == "up_philcoa") {
                                            viewModel.waitingCheckpointId.value = "up_sc"
                                            viewModel.formCheckpointId.value = "up_knl_gate"
                                        } else {
                                            viewModel.waitingCheckpointId.value = "cw_philcoa"
                                            viewModel.formCheckpointId.value = "cw_tandang"
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (route.isPilot) "🇵🇭 ${route.name.substringBefore("-")}" else route.name.substringBefore("-"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (route.isPilot) {
                                        Text(
                                            "PILOT",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Direction toggle buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val directions = listOf("Northbound", "Southbound")
                        directions.forEach { direction ->
                            val isSelected = direction == selectedDirection
                            Button(
                                onClick = { viewModel.selectedDirection.value = direction },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (direction == "Northbound") Icons.Default.VerticalAlignTop else Icons.Default.VerticalAlignBottom,
                                        contentDescription = direction,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (direction == "Northbound") "To UP (Northbound)" else "To LRT (Southbound)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Map Visualizer Panel
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "2. Interactive Corridor Map",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Simulator Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Simulate Rush Hour:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = isSimulatorRunning,
                                onCheckedChange = { viewModel.isSimulatorRunning.value = it },
                                thumbContent = {
                                    if (isSimulatorRunning) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                },
                                modifier = Modifier.scale(0.7f)
                            )
                        }
                    }

                    InteractiveMapCanvas(
                        checkpoints = checkpoints,
                        reports = recentReports,
                        waitingCheckpointId = waitingCheckpointId,
                        selectedDirection = selectedDirection,
                        onCheckpointSelected = { selectedId ->
                            viewModel.waitingCheckpointId.value = selectedId
                        }
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(SymbolsLocationMarkerPin(), contentDescription = null, tint = Color(0xFFFF5722), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Tap and select any stop on the map above to select where you are waiting downstream!",
                                fontSize = 10.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Predicted Downstream ETA widget
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "3. Your Estimated Waiting ETA",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (etaState) {
                                is PredictionState.Success -> MaterialTheme.colorScheme.primaryContainer
                                is PredictionState.AlreadyPassed -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = when (etaState) {
                                            is PredictionState.Success -> MaterialTheme.colorScheme.primary
                                            is PredictionState.AlreadyPassed -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.outline
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Your waiting stop:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = currentWaitingCp?.name ?: "Tap stop to select",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = when (etaState) {
                                        is PredictionState.Success -> MaterialTheme.colorScheme.onPrimaryContainer
                                        is PredictionState.AlreadyPassed -> MaterialTheme.colorScheme.onErrorContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            Divider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            when (val state = etaState) {
                                is PredictionState.Success -> {
                                    Text(
                                        text = "${state.etaMinutes} mins",
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "ETA PREDICTED DOWNSTREAM",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Reporting Vehicle:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                Text(state.reportingVehicleType, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Spotted At:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                Text(state.lastSeenCheckpoint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Passenger slots:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                Text(state.fullness, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (state.fullness.contains("standing")) Color(0xFFEF4444) else Color(0xFF10B981))
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Reported by:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                Text("${state.reporter} (${state.reportedAgoSeconds}s ago)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                                is PredictionState.AlreadyPassed -> {
                                    Icon(Icons.Default.Warning, contentDescription = "Passed icon", tint = Color(0xFFFF5722), modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Already Passed or Very Close",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Commuter '${state.lastType}' reported passing this gate ~${state.passedMinsAgo} min ago upstream. Keep looking out!",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                                else -> {
                                    Icon(Icons.Default.DirectionsBus, contentDescription = "Bus icon", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "No active check-ins upstream",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Traditional public transport doesn't share GPS. Activate 'Simulate Rush Hour' or tap the Report Live tool below to spawn simulated commuters who will show you how downstream calculations behave!",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Report live tool (Mark it on the route!)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "4. Report Live (Commuter Help Bench!)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Create, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Spot a jeep? Click to update downstream companions:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }

                            // Reporter Name
                            TextField(
                                value = formReporterName,
                                onValueChange = { viewModel.formReporterName.value = it },
                                label = { Text("Your Screen Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Select vehicle type
                            Column {
                                Text("Vehicle Class:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val types = listOf("Traditional Jeep", "Modern Jeep", "UV Express", "Bus")
                                    types.forEach { type ->
                                        val isSelected = formVehicleType == type
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { viewModel.formVehicleType.value = type }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                type.split(" ").first(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Select checkpoint seen at
                            Column {
                                Text("Where did you spot it / Where are you?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Let's filter first 4 checkpoints so it fits elegantly or select with box
                                    val spotCheckpoints = checkpoints.take(4)
                                    spotCheckpoints.forEach { cp ->
                                        val isSelected = formCheckpointId == cp.id
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { viewModel.formCheckpointId.value = cp.id }
                                                .padding(vertical = 8.dp, horizontal = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                cp.name.split(" ").firstOrNull{ it.length > 2 } ?: cp.name,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Fullness / Crowd Status
                            Column {
                                Text("Remaining Capacity:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val statList = listOf("Maluluwag", "May upuan pa", "Siksikan / standing")
                                    statList.forEach { statusText ->
                                        val isSelected = formCrowdStatus == statusText
                                        val bubbleColor = when (statusText) {
                                            "Maluluwag" -> Color(0xFF10B981)
                                            "May upuan pa" -> Color(0xFF3B82F6)
                                            else -> Color(0xFFEF4444)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isSelected) bubbleColor.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) bubbleColor else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { viewModel.formCrowdStatus.value = statusText }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                statusText.substringBefore(" /"),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) bubbleColor else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Submit Button
                            Button(
                                onClick = {
                                    viewModel.submitReport()
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Broadcast Community Check-In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Commuter boards tab (Live chatter feed & Ask AI)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "5. Commuter Boards & AI Bulletins",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { activeTab = 0 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💬 Live Commuter Feed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { activeTab = 1 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨ Ask AI Assistant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (activeTab == 0) {
                        // Community Chatter section
                        Card(
                            modifier = Modifier.fillMaxWidth().height(260.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Recent announcements on Route:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                                    // Spawner of chat alerts representing density
                                    TextButton(
                                        onClick = { viewModel.triggerAiChatterGeneration() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (GeminiService.isKeyAvailable()) "AI Spawn Chatter" else "Simulate Chatter",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (chatMessages.isEmpty()) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No messages yet. Write something below!", fontSize = 11.sp, fontStyle = FontStyle.Italic)
                                    }
                                } else {
                                    Box(modifier = Modifier.weight(1f)) {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxHeight(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(chatMessages) { chat ->
                                                val isGemini = chat.isAi || chat.sender.contains("Parapo")
                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isGemini) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                                        else MaterialTheme.colorScheme.surface
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = chat.sender,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isGemini) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                                )
                                                                if (chat.locationName != null) {
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text(
                                                                        "📍 ${chat.locationName}",
                                                                        fontSize = 8.sp,
                                                                        color = MaterialTheme.colorScheme.outline,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                            Text(
                                                                "Just now",
                                                                fontSize = 8.sp,
                                                                color = MaterialTheme.colorScheme.outline
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(3.dp))
                                                        Text(chat.text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Send manual chatter row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextField(
                                        value = chatMessageText,
                                        onValueChange = { chatMessageText = it },
                                        placeholder = { Text("Sabihin mo rito (e.g. Traffic sa Ateneo)...", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions(onSend = {
                                            viewModel.submitChatMessage(chatMessageText)
                                            chatMessageText = ""
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        })
                                    )

                                    IconButton(
                                        onClick = {
                                            viewModel.submitChatMessage(chatMessageText)
                                            chatMessageText = ""
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            .size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Send",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Ask Gemini AI Assistant
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ask Yaya Parapo AI (Street-Smart Concierge)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    "Our AI Assistant analyses active crowdsourced reports along $selectedDirection Corridor and provides real commuter decisions based on active waiting conditions!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )

                                TextField(
                                    value = aiQuestionText,
                                    onValueChange = { aiQuestionText = it },
                                    placeholder = { Text("e.g., Masikip ba sa Ateneo Gate 3 ngayon?", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        viewModel.askGemini(aiQuestionText)
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    })
                                )

                                Button(
                                    onClick = {
                                        viewModel.askGemini(aiQuestionText)
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    },
                                    enabled = aiQuestionText.isNotBlank() && !isAiLoading,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isAiLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Text("Advisahan Mo Ako, Yaya AI!", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Show response
                                AnimatedVisibility(
                                    visible = aiResponse != null || isAiLoading,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Yaya Parapo says:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                                if (!GeminiService.isKeyAvailable()) {
                                                    Text("OFFLINE MOCKED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                            if (isAiLoading) {
                                                Text("Thinking of witty advice in Katipunan...", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.outline)
                                            } else {
                                                Text(
                                                    text = aiResponse ?: "",
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom location marker icon helper
fun SymbolsLocationMarkerPin() = Icons.Default.Place
