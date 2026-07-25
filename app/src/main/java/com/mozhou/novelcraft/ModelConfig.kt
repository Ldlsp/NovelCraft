package com.mozhou.novelcraft

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ModelConfig(
    val provider: String = "OpenAI 兼容",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
)

class ModelPreferences(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "model_config",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun load(): ModelConfig = ModelConfig(
        provider = preferences.getString("provider", "OpenAI 兼容").orEmpty(),
        baseUrl = preferences.getString("base_url", "").orEmpty(),
        apiKey = preferences.getString("api_key", "").orEmpty(),
        model = preferences.getString("model", "").orEmpty(),
    )

    fun save(config: ModelConfig) {
        preferences.edit()
            .putString("provider", config.provider)
            .putString("base_url", config.baseUrl.trim().trimEnd('/'))
            .putString("api_key", config.apiKey.trim())
            .putString("model", config.model.trim())
            .apply()
    }
}

class OpenAiCompatibleClient {
    @Volatile private var activeConnection: HttpURLConnection? = null

    fun cancelActiveRequest() {
        activeConnection?.disconnect()
    }

    suspend fun test(config: ModelConfig): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(config.baseUrl.startsWith("https://")) { "Base URL 必须使用 HTTPS" }
            require(config.apiKey.isNotBlank()) { "请先填写 API Key" }
            val connection = URL(config.baseUrl.trimEnd('/') + "/models").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer " + config.apiKey)
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            val status = connection.responseCode
            connection.disconnect()
            if (status !in 200..299) error("接口返回 HTTP " + status)
            "连接成功"
        }
    }

    suspend fun continueWriting(config: ModelConfig, context: String): Result<String> = chat(
        config = config,
        context = context,
        temperature = 0.8,
        systemInstruction = "你是中文网文写作助手。只输出可直接接在正文后的小说正文，不输出标题、说明、Markdown 或分析。不得提前揭露尚未解决的核心谜底。",
    )

    suspend fun generateChapterPlan(config: ModelConfig, context: String): Result<String> = chat(
        config = config,
        context = context,
        temperature = 0.5,
        systemInstruction = "你是中文网文策划编辑。根据作者提供的已写内容和本地设定，只输出本章可执行大纲：目标、冲突升级、关键转折、结尾钩子。使用简短中文分点，不要写正文，不要暴露保密设定。",
    )

    private suspend fun chat(
        config: ModelConfig,
        context: String,
        temperature: Double,
        systemInstruction: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(config.baseUrl.startsWith("https://")) { "Base URL 必须使用 HTTPS" }
            require(config.apiKey.isNotBlank()) { "请先填写 API Key" }
            require(config.model.isNotBlank()) { "请先填写模型名称" }
            val body = JSONObject().apply {
                put("model", config.model)
                put("temperature", temperature)
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemInstruction))
                    put(JSONObject().put("role", "user").put("content", context))
                })
            }.toString()
            val connection = URL(config.baseUrl.trimEnd('/') + "/chat/completions").openConnection() as HttpURLConnection
            activeConnection = connection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Authorization", "Bearer " + config.apiKey)
                connection.connectTimeout = 20_000
                connection.readTimeout = 90_000
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) error("接口返回 HTTP " + status + ": " + response.take(180))
                JSONObject(response).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content").trim()
            } finally {
                if (activeConnection === connection) activeConnection = null
                connection.disconnect()
            }
        }
    }
}
