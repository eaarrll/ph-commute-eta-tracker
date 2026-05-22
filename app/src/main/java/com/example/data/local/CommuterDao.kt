package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.models.Checkpoint
import com.example.data.models.Route
import com.example.data.models.CheckInReport
import com.example.data.models.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface CommuterDao {

    @Query("SELECT * FROM routes")
    fun getAllRoutes(): Flow<List<Route>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<Route>)

    @Query("SELECT * FROM checkpoints WHERE routeId = :routeId ORDER BY orderIndex ASC")
    fun getCheckpointsForRoute(routeId: String): Flow<List<Checkpoint>>

    @Query("SELECT * FROM checkpoints WHERE routeId = :routeId ORDER BY orderIndex ASC")
    suspend fun getCheckpointsForRouteSync(routeId: String): List<Checkpoint>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpoints(checkpoints: List<Checkpoint>)

    @Query("SELECT * FROM check_in_reports WHERE routeId = :routeId AND direction = :direction AND timestamp > :sinceTimestamp ORDER BY timestamp DESC")
    fun getRecentReports(routeId: String, direction: String, sinceTimestamp: Long): Flow<List<CheckInReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: CheckInReport)

    @Query("DELETE FROM check_in_reports WHERE isSimulated = 1")
    suspend fun clearSimulatedReports()

    @Query("SELECT * FROM chat_messages WHERE routeId = :routeId ORDER BY timestamp DESC LIMIT 50")
    fun getRecentChats(routeId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteChatMessage(id: Int)
}
