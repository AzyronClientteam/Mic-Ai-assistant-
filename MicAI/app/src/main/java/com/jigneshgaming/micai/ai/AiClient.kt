package com.jigneshgaming.micai.ai

import com.jigneshgaming.micai.data.prefs.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Online AI brain. Talks to any OpenAI-compatible `/chat/completions`
 * endpoint (OpenAI, a self-hosted gateway, or the secure proxy shipped in
 * `MicAI/server/`). The API key comes from [SettingsStore] at runtime —
 * it is never baked into the APK.
 *
 * Supports text and vision (image) messages. Returns null on any failure
 * so the caller can fall back to the offline [LocalBrain].
 */
class AiClient(private val settings: SettingsStore) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean get() = settings.hasAiCredentials && !settings.localOnlyMode

    /**
     * @param history previous turns as (role, text) pairs, oldest first.
     * @param imageBase64 optional JPEG/PNG image (base64, no data-URI prefix)
     *        for vision questions such as "what is in this screenshot?".
     */
    suspend fun chat(
        history: List<Pair<String, String>>,
        userText: String,
        imageBase64: String? = null
    ): String? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        try {
            val messages = JSONArray()
            messages.put(
                JSONObject()
                    .put("role", "system")
                    .put("content", SYSTEM_PROMPT)
            )
            for ((role, text) in history.takeLast(20)) {
                messages.put(JSONObject().put("role", role).put("content", text))
            }
            val userContent: Any = if (imageBase64 != null) {
                JSONArray()
                    .put(
                        JSONObject()
                            .put("type", "text")
                            .put("text", userText.ifBlank { "Describe this image in detail." })
                    )
                    .put(
                        JSONObject()
                            .put("type", "image_url")
                            .put(
                                "image_url",
                                JSONObject().put("url", "data:image/jpeg;base64,$imageBase64")
                            )
                    )
            } else {
                userText
            }
            messages.put(JSONObject().put("role", "user").put("content", userContent))

            val body = JSONObject()
                .put("model", settings.aiModel)
                .put("messages", messages)
                .put("temperature", 0.7)
                .toString()

            val request = Request.Builder()
                .url(settings.aiBaseUrl.trimEnd('/') + "/chat/completions")
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer ${settings.aiApiKey}")
                .header("Content-Type", "application/json")
                .build()

            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val text = response.body?.string() ?: return@withContext null
                val json = JSONObject(text)
                val choices = json.optJSONArray("choices") ?: return@withContext null
                if (choices.length() == 0) return@withContext null
                val message = choices.getJSONObject(0).optJSONObject("message")
                    ?: return@withContext null
                val content = message.optString("content")
                if (content.isBlank()) null else content
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val SYSTEM_PROMPT =
            "You are MIC, a personal AI assistant created by JIGNESH GAMING, " +
                "inspired by JARVIS. You are running on the user's Android phone. " +
                "Be concise, friendly and practical. Answer in the same language " +
                "the user writes in (English, Hindi, Gujarati or others). " +
                "You can help with: general knowledge, coding and programming, " +
                "building websites and apps, Minecraft Java and Bedrock, Discord " +
                "bot development, VPS and server management, studying, writing, " +
                "planning multi-step tasks, and analysing images or documents the " +
                "user shares. When the user asks you to do something on the phone " +
                "(open an app, set a reminder, take a note, check battery), the " +
                "app itself handles it locally — just confirm what was done."
    }
}
