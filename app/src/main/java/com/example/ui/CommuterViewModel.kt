package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.gemini.GeminiService
import com.example.data.models.Checkpoint
import com.example.data.models.Route
import com.example.data.models.CheckInReport
import com.example.data.models.ChatMessage
import com.example.data.repository.CommuterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed interface PredictionState {
    object Idle : PredictionState
    object Loading : PredictionState
    data class Success(
        val etaMinutes: Int,
        val distanceKm: Double,
        val reportingVehicleType: String,
        val lastSeenCheckpoint: String,
        val reportedAgoSeconds: Int,
        val fullness: String,
        val reporter: String
    ) : PredictionState
    data class AlreadyPassed(val lastType: String, val passedMinsAgo: Int) : PredictionState
    object NoData : PredictionState
}

class CommuterViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = CommuterRepository(db.commuterDao())

    // UI Input States
    val selectedRouteId = MutableStateFlow("katipunan_cubao")
    val selectedDirection = MutableStateFlow("Northbound") // "Northbound" or "Southbound"
    val waitingCheckpointId = MutableStateFlow("kc_uptc") // User is waiting here

    val isSimulatorRunning = MutableStateFlow(true) // Default to true so preview is immediately action-packed!
    val isAiLoading = MutableStateFlow(false)
    val aiResponse = MutableStateFlow<String?>(null)

    // Form states
    val formVehicleType = MutableStateFlow("Traditional Jeep")
    val formCheckpointId = MutableStateFlow("kc_ateneo")
    val formCrowdStatus = MutableStateFlow("May upuan pa")
    val formReporterStatus = MutableStateFlow("I'm on board")
    val formReporterName = MutableStateFlow("IskoCommuter")

    // Database flows mapping inputs
    val routes: StateFlow<List<Route>> = repository.allRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val checkpoints: StateFlow<List<Checkpoint>> = selectedRouteId
        .flatMapLatest { routeId -> repository.getCheckpoints(routeId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentReports: StateFlow<List<CheckInReport>> = combine(
        selectedRouteId, selectedDirection
    ) { routeId, direction ->
        Pair(routeId, direction)
    }.flatMapLatest { (routeId, direction) ->
        repository.getRecentReports(routeId, direction, windowMs = 600000) // 10 minutes window
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = selectedRouteId
        .flatMapLatest { routeId -> repository.getRecentChats(routeId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic Interpolated ETA calculation
    val etaPrediction: StateFlow<PredictionState> = combine(
        checkpoints, recentReports, waitingCheckpointId, selectedDirection
    ) { cps, reports, waitingId, direction ->
        calculatePrediction(cps, reports, waitingId, direction)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PredictionState.NoData)

    // Simulation Job
    private var simulationJob: Job? = null
    // Position of simulated vehicles (0.0 to 1.0 mapping along route index)
    private var simVehicles = mutableListOf<SimVehicle>()

    data class SimVehicle(
        val id: String,
        val vehicleType: String,
        val fullness: String,
        var currentProgress: Double, // 0.0 to 1.0 representing route completion
        val reporterName: String,
        val speedFactor: Double // speed multiplier
    )

    init {
        // Initialize 3 virtual vehicles for Katipunan-Cubao
        resetSimulationVehicles()

        // Start Simulator active loop
        viewModelScope.launch {
            isSimulatorRunning.collect { running ->
                if (running) {
                    startSimulationLoop()
                } else {
                    stopSimulationLoop()
                }
            }
        }
    }

    private fun resetSimulationVehicles() {
        simVehicles = mutableListOf(
            SimVehicle("sim_jeep_1", "Traditional Jeep", "Siksikan / standing", 0.05, "Manong Driver", 0.8),
            SimVehicle("sim_mjeep_2", "Modern Jeep", "May upuan pa", 0.35, "AteneoStaff_02", 1.1),
            SimVehicle("sim_uv_3", "UV Express", "Maluluwag", 0.65, "CarpoolSuki", 0.95)
        )
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(3000) // tick every 3 seconds
                updateSimulatedVehicles()
            }
        }
    }

    private fun stopSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = null
        viewModelScope.launch {
            repository.clearSimulatedReports()
        }
    }

    private fun updateSimulatedVehicles() {
        val cps = checkpoints.value
        if (cps.isEmpty()) return

        val routeId = selectedRouteId.value
        val direction = selectedDirection.value

        viewModelScope.launch {
            simVehicles.forEach { vehicle ->
                // Progress vehicles forward
                vehicle.currentProgress += 0.04 * vehicle.speedFactor
                if (vehicle.currentProgress > 1.0) {
                    // Reset to the beginning of the route
                    vehicle.currentProgress = 0.0
                }

                // Map progress percentage to checkpoints list
                // For Northbound: orderIndex asc (0 -> 5)
                // For Southbound: orderIndex desc (5 -> 0)
                val orderedCps = if (direction == "Northbound") cps else cps.reversed()
                val cpCount = orderedCps.size
                if (cpCount > 1) {
                    // Find which checkpoints the vehicle matches along route
                    val rawIndexDouble = vehicle.currentProgress * (cpCount - 1)
                    val targetCell = rawIndexDouble.toInt().coerceIn(0, cpCount - 1)
                    val checkpoint = orderedCps[targetCell]

                    // Trigger simulated report once it hits a checkpoint cell
                    // To prevent clogging database, we replace previous report
                    repository.insertReport(
                        CheckInReport(
                            routeId = routeId,
                            vehicleType = vehicle.vehicleType,
                            checkpointId = checkpoint.id,
                            direction = direction,
                            crowdStatus = vehicle.fullness,
                            timestamp = System.currentTimeMillis(),
                            reporterName = vehicle.reporterName,
                            reporterStatus = "I'm on board",
                            isSimulated = true
                        )
                    )
                }
            }
        }
    }

    private fun calculatePrediction(
        cps: List<Checkpoint>,
        reports: List<CheckInReport>,
        waitingId: String,
        direction: String
    ): PredictionState {
        if (cps.isEmpty() || reports.isEmpty()) return PredictionState.NoData

        val waitingCp = cps.find { it.id == waitingId } ?: return PredictionState.NoData
        val waitingIndex = cps.indexOf(waitingCp)

        // Filter reports that are on board or seen passing, not simulated duplicates
        val activeReports = reports.filter { it.routeId == selectedRouteId.value && it.direction == direction }
        if (activeReports.isEmpty()) return PredictionState.NoData

        // Define route traversal order
        val orderedCps = if (direction == "Northbound") cps else cps.reversed()
        val waitingRelativeIndex = orderedCps.indexOf(waitingCp)

        if (waitingRelativeIndex == -1) return PredictionState.NoData

        // Find reports that are UPSTREAM of the user's waiting position
        // Upstream means the vehicle is at a checkpoint whose index in orderedCps is LESS than the user's index.
        val upstreamReports = activeReports.filter { report ->
            val reportCp = cps.find { it.id == report.checkpointId }
            if (reportCp != null) {
                val reportRelativeIndex = orderedCps.indexOf(reportCp)
                reportRelativeIndex != -1 && reportRelativeIndex <= waitingRelativeIndex
            } else {
                false
            }
        }

        // Find reports that already PASSED the user's waiting position (DOWNSTREAM)
        val downstreamReports = activeReports.filter { report ->
            val reportCp = cps.find { it.id == report.checkpointId }
            if (reportCp != null) {
                val reportRelativeIndex = orderedCps.indexOf(reportCp)
                reportRelativeIndex > waitingRelativeIndex
            } else {
                false
            }
        }

        if (upstreamReports.isEmpty()) {
            if (downstreamReports.isNotEmpty()) {
                val mostRecentDownstream = downstreamReports.minByOrNull { System.currentTimeMillis() - it.timestamp }
                if (mostRecentDownstream != null) {
                    val passTimeMins = ((System.currentTimeMillis() - mostRecentDownstream.timestamp) / 60000).toInt()
                    return PredictionState.AlreadyPassed(mostRecentDownstream.vehicleType, passTimeMins.coerceAtLeast(1))
                }
            }
            return PredictionState.NoData
        }

        // Get the closest upstream report.
        // Closest means reportRelativeIndex is as close as possible to waitingRelativeIndex (but still <=).
        val bestReport = upstreamReports.maxByOrNull { report ->
            val cp = cps.find { it.id == report.checkpointId }!!
            orderedCps.indexOf(cp)
        } ?: return PredictionState.NoData

        val bestReportCp = cps.find { it.id == bestReport.checkpointId }!!
        val distanceDiff = Math.abs(waitingCp.distanceKmFromStart - bestReportCp.distanceKmFromStart)

        // Speed calculation based on public vehicle traffic (average 15 km/h stop-and-go)
        val kmPerMinute = 15.0 / 60.0 // 0.25 km/min
        val transitTimeMinutes = distanceDiff / kmPerMinute

        val elapsedMinutes = ((System.currentTimeMillis() - bestReport.timestamp) / 60000.0)
        val remainingMinutes = (transitTimeMinutes - elapsedMinutes).toInt()

        val reportedAgoSecs = ((System.currentTimeMillis() - bestReport.timestamp) / 1000).toInt()

        return if (remainingMinutes <= 0) {
            PredictionState.Success(
                etaMinutes = 1,
                distanceKm = distanceDiff,
                reportingVehicleType = bestReport.vehicleType,
                lastSeenCheckpoint = bestReportCp.name,
                reportedAgoSeconds = reportedAgoSecs,
                fullness = bestReport.crowdStatus,
                reporter = bestReport.reporterName
            )
        } else {
            PredictionState.Success(
                etaMinutes = remainingMinutes,
                distanceKm = distanceDiff,
                reportingVehicleType = bestReport.vehicleType,
                lastSeenCheckpoint = bestReportCp.name,
                reportedAgoSeconds = reportedAgoSecs,
                fullness = bestReport.crowdStatus,
                reporter = bestReport.reporterName
            )
        }
    }

    // Community Report actions
    fun submitReport() {
        val routeId = selectedRouteId.value
        val direction = selectedDirection.value
        val cpId = formCheckpointId.value
        val vehicleType = formVehicleType.value
        val crowd = formCrowdStatus.value
        val status = formReporterStatus.value
        val name = formReporterName.value.ifBlank { "UnidentifiedCommuter" }

        viewModelScope.launch {
            repository.insertReport(
                CheckInReport(
                    routeId = routeId,
                    vehicleType = vehicleType,
                    checkpointId = cpId,
                    direction = direction,
                    crowdStatus = crowd,
                    timestamp = System.currentTimeMillis(),
                    reporterName = name,
                    reporterStatus = status,
                    isSimulated = false
                )
            )

            // Submit an automatic message to commuter chat logging the check-in!
            val cpName = checkpoints.value.find { it.id == cpId }?.name ?: cpId
            repository.insertChatMessage(
                ChatMessage(
                    routeId = routeId,
                    sender = name,
                    text = "📢 Live: Marked $vehicleType sitting '$crowd' departing $cpName toward ${direction}!",
                    timestamp = System.currentTimeMillis(),
                    locationName = cpName
                )
            )
        }
    }

    // Commuter Chat Actions
    fun submitChatMessage(text: String) {
        if (text.isBlank()) return
        val routeId = selectedRouteId.value
        val cpId = waitingCheckpointId.value
        val cpName = checkpoints.value.find { it.id == cpId }?.name ?: ""
        val sender = formReporterName.value.ifBlank { "AnonCommuter" }

        viewModelScope.launch {
            repository.insertChatMessage(
                ChatMessage(
                    routeId = routeId,
                    sender = sender,
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    locationName = cpName
                )
            )
        }
    }

    // Ask Gemini AI Commuter Concierge
    fun askGemini(question: String) {
        if (question.isBlank()) return
        isAiLoading.value = true
        aiResponse.value = null

        viewModelScope.launch {
            val response = GeminiService.getCommuterAdvice(
                question = question,
                activeReports = recentReports.value,
                checkpoints = checkpoints.value,
                currentDirection = selectedDirection.value
            )
            aiResponse.value = response
            isAiLoading.value = false

            // Also post Gemini reply to Chat so other waiting commuters see the bulletin!
            repository.insertChatMessage(
                ChatMessage(
                    routeId = selectedRouteId.value,
                    sender = "Yaya Parapo (AI Assistant)",
                    text = response,
                    timestamp = System.currentTimeMillis(),
                    isAi = true,
                    locationName = "Cloud Oracle"
                )
            )
        }
    }

    // Generate AI Chatter
    fun triggerAiChatterGeneration() {
        viewModelScope.launch {
            val routeId = selectedRouteId.value
            val routeName = routes.value.find { it.id == routeId }?.name ?: "Katipunan"
            val cps = checkpoints.value
            if (cps.isEmpty()) return@launch

            val objects = GeminiService.generateSyntheticChatter(routeId, routeName, cps)
            objects.forEach { obj ->
                val sender = obj.optString("sender", "Ka-Commuter")
                val text = obj.optString("text")
                val loc = obj.optString("locationName")

                repository.insertChatMessage(
                    ChatMessage(
                        routeId = routeId,
                        sender = sender,
                        text = text,
                        timestamp = System.currentTimeMillis() - (Math.random() * 120000).toLong(), // slightly in past
                        locationName = loc
                    )
                )
            }
        }
    }

    fun clearAllReports() {
        viewModelScope.launch {
            repository.clearSimulatedReports()
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
    }
}
