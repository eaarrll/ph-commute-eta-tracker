package com.example.data.repository

import com.example.data.local.CommuterDao
import com.example.data.models.Checkpoint
import com.example.data.models.Route
import com.example.data.models.CheckInReport
import com.example.data.models.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommuterRepository(private val dao: CommuterDao) {

    val allRoutes: Flow<List<Route>> = dao.getAllRoutes()

    fun getCheckpoints(routeId: String): Flow<List<Checkpoint>> {
        return dao.getCheckpointsForRoute(routeId)
    }

    suspend fun getCheckpointsSync(routeId: String): List<Checkpoint> = withContext(Dispatchers.IO) {
        dao.getCheckpointsForRouteSync(routeId)
    }

    fun getRecentReports(routeId: String, direction: String, windowMs: Long = 1800000): Flow<List<CheckInReport>> {
        // Look back at reports up to 30 mins (1800000 ms) ago to estimate ETAs
        val minTime = System.currentTimeMillis() - windowMs
        return dao.getRecentReports(routeId, direction, minTime)
    }

    fun getRecentChats(routeId: String): Flow<List<ChatMessage>> {
        return dao.getRecentChats(routeId)
    }

    suspend fun insertReport(report: CheckInReport) = withContext(Dispatchers.IO) {
        dao.insertReport(report)
    }

    suspend fun insertChatMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        dao.insertChatMessage(message)
    }

    suspend fun clearSimulatedReports() = withContext(Dispatchers.IO) {
        dao.clearSimulatedReports()
    }

    suspend fun deleteChat(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteChatMessage(id)
    }

    // Force seed database if table is empty (secondary fallback to Callback)
    suspend fun ensureDatabaseSeeded() = withContext(Dispatchers.IO) {
        // Can be queried on app startup
    }
}
