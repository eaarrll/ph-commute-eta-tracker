package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.data.models.CheckInReport
import com.example.data.models.Checkpoint
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if the API key is set and valid (not default placeholder)
     */
    fun isKeyAvailable(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.startsWith("placeholder", ignoreCase = true)
    }

    /**
     * Ask Gemini a general commuter-related question, feeding it active crowdsourced reports
     */
    suspend fun getCommuterAdvice(
        question: String,
        activeReports: List<CheckInReport>,
        checkpoints: List<Checkpoint>,
        currentDirection: String
    ): String = withContext(Dispatchers.IO) {
        if (!isKeyAvailable()) {
            return@withContext getMockAdvice(question, activeReports, currentDirection)
        }

        try {
            // Document current reports matching checkpoints
            val reportSummary = if (activeReports.isEmpty()) {
                "No active commuter check-ins in the last 30 minutes."
            } else {
                activeReports.joinToString("\n") { r ->
                    val cpName = checkpoints.find { it.id == r.checkpointId }?.name ?: r.checkpointId
                    "- ${r.vehicleType} at $cpName going ${r.direction}. Status: ${r.crowdStatus} (reported by ${r.reporterName} ${getRelativeTimeText(r.timestamp)})"
                }
            }

            val systemPrompt = """
                You are "Yaya Parapo", an expert, friendly, street-smart Filipino AI Commuter Concierge.
                Your purpose is to help commuters coordinate their travel along major GPS-less routes (like Katipunan Avenue).
                You speak in natural "Taglish" (Tagalog-English mix) with commuter slang (e.g., "Para po", "Siksikan", "Sabit", "May upuan", "Berdeng jeep", "Estudyante flow").
                Be encouraging, direct, extremely witty, and highly knowledgeable about local traffic patterns.
                
                Here are the active crowdsourced transport reports on the selected route:
                $reportSummary
                
                Respond to the user's question, giving advice on whether they should wait, walk to another gate, try to ride traditional vs modern, or use other routes if traffic is terrible. Keep response under 4 paragraphs. Speak like a real helpful Filipino commute partner!
            """.trimIndent()

            val responseText = executeGeminiRequest(systemPrompt, question)
            return@withContext responseText ?: "Naku pasensya na, para bang mahina ang signal ko ngayon. Subukan natin ulit mamaya!"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting advice from Gemini", e)
            return@withContext "Mukhang nagka-error sa biyahe ng AI: ${e.localizedMessage}. Balik tayo sa local simulation!"
        }
    }

    /**
     * Ask Gemini to generate 3 realistic commuter updates to pre-populate or simulate crowd density
     */
    suspend fun generateSyntheticChatter(routeId: String, routeName: String, checkpoints: List<Checkpoint>): List<JSONObject> = withContext(Dispatchers.IO) {
        val fallbackList = getMockSyntheticChatter(routeId, checkpoints)
        if (!isKeyAvailable()) {
            return@withContext fallbackList
        }

        try {
            val checkpointsListStr = checkpoints.joinToString(", ") { it.name }
            val systemPrompt = """
                You are a generator for commuter status chatter on a crowdsourced app called "Para Po!".
                The transport route is: $routeName.
                The key stops are: $checkpointsListStr.
                Please generate 3 individual, realistic commuter chat alerts.
                Each report must sound human, written in realistic Taglish or Filipino. Talk about delays, weather, terminal queues, modern vs traditional jeepney issues, student/work crowd flow, or witty local commuter commentary.
                Return ONLY a JSON Array composed of 3 objects. Do not write markdown tags blocks like ```json or similar, just the raw valid JSON.
                Each JSON object must have keys:
                - "sender": a realistic Filipino commuter name (e.g., "StudentDianne", "KuyaJoel", "AteneoPapi99", "AnakNgBayan", "CommuterQueen")
                - "text": the message text in engaging Taglish/Filipino (under 140 characters)
                - "locationName": one of the exact stop names from our list ($checkpointsListStr) where this person is likely located.
            """.trimIndent()

            val rawOutput = executeGeminiRequest(systemPrompt, "Sige, simulan mo na ang biyahe!")
            if (rawOutput != null) {
                // Parse the array
                val cleanedOutput = cleanJsonMarkdown(rawOutput)
                val jsonArray = JSONArray(cleanedOutput)
                val result = mutableListOf<JSONObject>()
                for (i in 0 until jsonArray.length()) {
                    result.add(jsonArray.getJSONObject(i))
                }
                if (result.isNotEmpty()) return@withContext result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating synthetic chatter", e)
        }
        return@withContext fallbackList
    }

    private fun cleanJsonMarkdown(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.substring(7)
        } else if (str.startsWith("```")) {
            str = str.substring(3)
        }
        if (str.endsWith("```")) {
            str = str.substring(0, str.length - 3)
        }
        return str.trim()
    }

    private suspend fun executeGeminiRequest(systemInstruction: String, userMessage: String): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val url = "$BASE_URL?key=$apiKey"

        val jsonRequest = JSONObject()
        
        // Contents
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", userMessage)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        jsonRequest.put("contents", contentsArray)

        // System Instruction
        val systemInstructionObj = JSONObject()
        val sysPartsArray = JSONArray()
        val sysPartObj = JSONObject()
        sysPartObj.put("text", systemInstruction)
        sysPartsArray.put(sysPartObj)
        systemInstructionObj.put("parts", sysPartsArray)
        jsonRequest.put("systemInstruction", systemInstructionObj)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini call failed with code: ${response.code}, body: ${response.body?.string()}")
                return null
            }
            val bodyString = response.body?.string() ?: return null
            val responseJson = JSONObject(bodyString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidateObj = candidates.getJSONObject(0)
                val content = candidateObj.optJSONObject("content")
                if (content != null) {
                    val parts = content.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text")
                    }
                }
            }
        }
        return null
    }

    // Fallbacks
    private fun getMockAdvice(question: String, activeReports: List<CheckInReport>, currentDirection: String): String {
        val cleanQ = question.lowercase()
        return when {
            cleanQ.contains("lrt") || cleanQ.contains("terminal") -> {
                "Hoy bes, kung nasa LRT terminal ka ngayon, medyo mahaba raw ang pila ng traditional jeepney. " +
                        "Ayon sa huling crowd counts natin, siksikan na ang biyahe ngunit may modern jeep na parating na galing Aurora Corner! If nagmamadali ka, baka mas mabilis mag-tricycle sa pilahan sa tabi, pero 15 pesos special."
            }
            cleanQ.contains("traffic") || cleanQ.contains("trapik") || cleanQ.contains("heavy") -> {
                "Traffic report check: as usual, masikip dyan sa tapat ng Miriam College footbridge and UP Town entry gawa ng construction tsaka school rush hour flow. " +
                        "If you're heading Northbound (UP Town), patience is a virtue! Better wait for 5 more minutes bago sumalang, or better, walk a bit hanggang sa Ateneo gym flyover para makasilong."
            }
            cleanQ.contains("ateneo") || cleanQ.contains("admu") -> {
                "Kumusta commuter ng Blue Eagle! Sa Ateneo Gate 3, medyo marami ang nag-aabang ng Southbound pa-LRT. Meron tayong nakatenggang modern jeepney doon, pero nagpuno pa. " +
                        "Northbound traditional jeeps are moving smoothly every 4 minutes. May modern jeep din na malapit na dyan."
            }
            cleanQ.contains("up") || cleanQ.contains("diliman") -> {
                "Para sa aming UP Iskolar, okay naman ang daloy ng traditional jeep sa Shuster gate. Southbound pa-LRT Katipunan may katamtamang space pa. " +
                        "If you are exiting the campus via Philcoa, mas sunod-sunod ang jeep doon. Maluwag pa!"
            }
            else -> {
                "Uy commuter buddy! Ayon sa system data natin para sa rotang ito, optimal naman ang transport density. " +
                        "${if (activeReports.isEmpty()) "Wala pa masyadong reports dyan sa checkpoint mo ngayon, pero may active simulation tayo." else "May reports tayong natanggap mula kay ${activeReports.firstOrNull()?.reporterName ?: "Kuya Driver"}"} " +
                        "Kung nag-aabang ka, relax lang at siguraduhing nasa tamang loading bay para hindi mahuli ng MMDA!"
            }
        }
    }

    private fun getMockSyntheticChatter(routeId: String, checkpoints: List<Checkpoint>): List<JSONObject> {
        val list = mutableListOf<JSONObject>()
        val cp1 = checkpoints.getOrNull(0)?.name ?: "Terminal"
        val cp2 = checkpoints.getOrNull(checkpoints.size / 2)?.name ?: "Ateneo"
        val cp3 = checkpoints.getOrNull(checkpoints.size - 1)?.name ?: "UP Campus"

        val obj1 = JSONObject()
        obj1.put("sender", "Katip_Pioneer")
        obj1.put("text", "Puno na agad traditional jeep sa terminal! Buti may modern jeep na sumunod, may upuan pa.")
        obj1.put("locationName", cp1)
        list.add(obj1)

        val obj2 = JSONObject()
        obj2.put("sender", "Atenista_Girl")
        obj2.put("text", "Super bagal ng traffic sa flyover corridor gawa ng unloading of buses. Plan ahead guys!")
        obj2.put("locationName", cp2)
        list.add(obj2)

        val obj3 = JSONObject()
        obj3.put("sender", "Diliman_Dreamer")
        obj3.put("text", "Just spotted 2 modern jeeps moving Southbound here. Looks half empty, grab them downstream!")
        obj3.put("locationName", cp3)
        list.add(obj3)

        return list
    }

    private fun getRelativeTimeText(timestamp: Long): String {
        val diffSec = (System.currentTimeMillis() - timestamp) / 1000
        return when {
            diffSec < 60 -> "just now"
            diffSec < 3600 -> "${diffSec / 60} mins ago"
            else -> "${diffSec / 3600} hrs ago"
        }
    }
}
