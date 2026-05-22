package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.models.Checkpoint
import com.example.data.models.Route
import com.example.data.models.CheckInReport
import com.example.data.models.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Route::class, Checkpoint::class, CheckInReport::class, ChatMessage::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun commuterDao(): CommuterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "para_po_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.commuterDao())
                }
            }
        }

        private suspend fun populateDatabase(dao: CommuterDao) {
            // Seed Routes
            val routes = listOf(
                Route(
                    id = "katipunan_cubao",
                    name = "Katipunan-Cubao LRT2",
                    description = "Pilot corridor showing real-time GPS-less crowdsourced updates along Katipunan Avenue.",
                    isPilot = true
                ),
                Route(
                    id = "up_philcoa",
                    name = "UP-Philcoa Loop",
                    description = "Inside-to-outside campus loop connecting UP Diliman with Philcoa Hub.",
                    isPilot = false
                ),
                Route(
                    id = "commonwealth_quiapo",
                    name = "Commonwealth-Quiapo Bus Line",
                    description = "Major public highway transport corridor for provincial and express buses.",
                    isPilot = false
                )
            )
            dao.insertRoutes(routes)

            // Seed Checkpoints for Katipunan-Cubao LRT2
            val katipunanCheckpoints = listOf(
                Checkpoint("kc_lrt", "katipunan_cubao", "LRT2 Katipunan Terminal", 0, 0.0, "Underpass Terminal Plaza"),
                Checkpoint("kc_aurora", "katipunan_cubao", "Aurora Blvd Corner", 1, 0.4, "Katipunan Flyover Underpass"),
                Checkpoint("kc_ateneo", "katipunan_cubao", "Ateneo Gate 3", 2, 1.1, "Blue Eagle Gym & Gate 3 Terminal"),
                Checkpoint("kc_miriam", "katipunan_cubao", "Miriam College Gate", 3, 1.8, "Pedestrian Footbridge"),
                Checkpoint("kc_uptc", "katipunan_cubao", "UP Town Center", 4, 2.4, "Red Phase Loading/Unloading Bay"),
                Checkpoint("kc_shuster", "katipunan_cubao", "UP Diliman (Shuster Gate)", 5, 3.2, "University Avenue Checkpoint")
            )
            dao.insertCheckpoints(katipunanCheckpoints)

            // Seed Checkpoints for UP-Philcoa Loop
            val upCheckpoints = listOf(
                Checkpoint("up_phil_term", "up_philcoa", "Philcoa Terminal", 0, 0.0, "Behind Citimall"),
                Checkpoint("up_knl_gate", "up_philcoa", "UP Gate (KNL Corner)", 1, 1.1, "Guardhouse Post"),
                Checkpoint("up_sc", "up_philcoa", "UP Shopping Center", 2, 1.9, "A2 Dorm Area Street Food"),
                Checkpoint("up_melchor", "up_philcoa", "Melchor Hall", 3, 2.5, "College of Engineering"),
                Checkpoint("up_oblation", "up_philcoa", "Quezon Hall (Oblation Plaza)", 4, 3.1, "Oblation Statue Roundabout")
            )
            dao.insertCheckpoints(upCheckpoints)

            // Seed Checkpoints for Commonwealth-Quiapo Bus Line
            val cwCheckpoints = listOf(
                Checkpoint("cw_regalado", "commonwealth_quiapo", "Regalado Highway", 0, 0.0, "SM Fairview Junction"),
                Checkpoint("cw_sandigan", "commonwealth_quiapo", "Sandiganbayan Stop", 1, 3.5, "Government Plazas"),
                Checkpoint("cw_tandang", "commonwealth_quiapo", "Tandang Sora Overpass", 2, 6.2, "Commonwealth Market"),
                Checkpoint("cw_philcoa", "commonwealth_quiapo", "Philcoa Flyover", 3, 8.5, "Citibank Hub/University Ave"),
                Checkpoint("cw_welcome", "commonwealth_quiapo", "Welcome Rotonda", 4, 13.0, "QC-Manila Boundary"),
                Checkpoint("cw_quiapo", "commonwealth_quiapo", "Quiapo Church Terminal", 5, 16.5, "Under the Plaza")
            )
            dao.insertCheckpoints(cwCheckpoints)

            // Seed initial local chats with authentic Filipino commuter experience
            val initialChats = listOf(
                ChatMessage(
                    routeId = "katipunan_cubao",
                    sender = "Kuya Marlon",
                    text = "Grabe haba ng pila ngayon dito sa lrt terminal. Puno kagad mga jeep pa-UP.",
                    timestamp = System.currentTimeMillis() - 600000,
                    locationName = "LRT2 Katipunan Terminal"
                ),
                ChatMessage(
                    routeId = "katipunan_cubao",
                    sender = "Maria_Atenista",
                    text = "Has anyone spotted a modern jeep? Downstream waiting at Ateneo Gate 3, getting late for class.",
                    timestamp = System.currentTimeMillis() - 420000,
                    locationName = "Ateneo Gate 3"
                ),
                ChatMessage(
                    routeId = "katipunan_cubao",
                    sender = "Carlos_UP",
                    text = "Rain starting to fall. Matumal ang biyahe, traffic din along Miriam College. Ingat guys!",
                    timestamp = System.currentTimeMillis() - 180000,
                    locationName = "Miriam College Gate"
                )
            )
            for (chat in initialChats) {
                dao.insertChatMessage(chat)
            }
        }
    }
}
