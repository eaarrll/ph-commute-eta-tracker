package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class Route(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isPilot: Boolean = false
)

@Entity(tableName = "checkpoints")
data class Checkpoint(
    @PrimaryKey val id: String,
    val routeId: String,
    val name: String,
    val orderIndex: Int,
    val distanceKmFromStart: Double, // Cumulative distance along route for ETA speed math
    val landmark: String
)

@Entity(tableName = "check_in_reports")
data class CheckInReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routeId: String,
    val vehicleType: String, // "Traditional Jeep", "Modern Jeep", "UV Express", "Mini Bus", "Bus"
    val checkpointId: String, // Where the vehicle is/was spotted
    val direction: String, // "Northbound", "Southbound"
    val crowdStatus: String, // "Siksikan / standing", "May upuan pa", "Maluluwag"
    val timestamp: Long,
    val reporterName: String,
    val reporterStatus: String, // "I'm on board", "Just saw it pass", "Spotted it waiting"
    val isSimulated: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routeId: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val isAi: Boolean = false,
    val locationName: String? = null
)
