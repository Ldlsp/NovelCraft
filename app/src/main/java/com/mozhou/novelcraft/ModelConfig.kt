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
    val imageBaseUrl: String = "",
    val imageApiKey: String = "",
    val imageModel: String = "",
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
        imageBaseUrl = preferences.getString("image_base_url", "").orEmpty(),
        imageApiKey = preferences.getString("image_api_key", "").orEmpty(),
        imageModel = preferences.getString("image_model", "").orEmpty(),
    )

    fun save(config: ModelConfig) {
        preferences.edit()
            .putString("provider", config.provider)
            .putString("base_url", config.baseUrl.trim().trimEnd('/'))
            .putString("api_key", config.apiKey.trim())
            .putString("model", config.model.trim())
            .putString("image_base_url", config.imageBaseUrl.trim().trimEnd('/'))
            .putString("image_api_key", config.imageApiKey.trim())
            .putString("image_model", config.imageModel.trim())
            .apply()
    }
}

class OpenAiCompatibleClient {
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

    suspend fun continueWriting(config: ModelConfig, context: String, request: GenerationRequest? = null): Result<String> = chat(
        config = config,
        context = context,
        temperature = 0.8,
        systemInstruction = "你是中文网文写作助手。只输出可直接接在正文后的小说正文，不输出标题、说明、Markdown 或分析。不得提前揭露尚未解决的核心谜底。",
        request = request,
    )

    suspend fun generateChapterPlan(config: ModelConfig, context: String, request: GenerationRequest? = null): Result<String> = chat(
        config = config,
        context = context,
        temperature = 0.5,
        systemInstruction = "你是中文网文策划编辑。根据作者提供的已写内容和本地设定，只输出本章可执行大纲：目标、冲突升级、关键转折、结尾钩子。使用简短中文分点，不要写正文，不要暴露保密设定。",
        request = request,
    )

    suspend fun generateBeatSheet(config: ModelConfig, context: String, request: GenerationRequest? = null): Result<String> = chat(
        config = config,
        context = context,
        temperature = 0.4,
        systemInstruction = "你是中文网文分镜策划。基于本章计划、锚点和历史信息，只输出 4-7 条按顺序执行的 Beat Sheet。每条必须写明场景/人物动作/信息变化或冲突升级；最后一条必须是具体钩子。不要写正文、分析或 Markdown 标题；不得提前揭露禁区。",
        request = request,
    )

    suspend fun writeFullChapter(config: ModelConfig, context: String, request: GenerationRequest? = null): Result<String> = chat(
        config = config,
        context = context,
        temperature = 0.8,
        systemInstruction = "你是中文网文作者。根据上下文、文风、锚点和分镜，输出一章完整纯小说正文（建议不少于 800 字）。只输出正文，不输出标题、说明、Markdown、分析或元信息。不得提前揭露禁区，结尾必须留下具体可继续写的钩子。",
        request = request,
    )

    suspend fun extractStoryMemory(config: ModelConfig, chapterText: String, request: GenerationRequest? = null): Result<String> = chat(
        config = config,
        context = chapterText,
        temperature = 0.1,
        systemInstruction = """你是小说知识图谱抽取器。仅根据给出的章节文本提取明确出现或明确变化的信息，不得猜测、补全或写小说正文。只输出一个合法 JSON 对象，不要 Markdown：
{
  "items":[{"kind":"人物|地点|势力|物品|事件|伏笔|世界规则","name":"名称","detail":"本章可验证的状态或事实","status":"活跃|已回收|保密"}],
  "edges":[{"source":"已在items中出现的名称","target":"已在items中出现的名称","relation":"同盟|敌对|位于|持有|隶属|触发|铺垫|师徒|情感","description":"本章证据"}]
}
没有可靠信息时返回空数组。每类最多15条，不要把普通路人、泛称或推测当实体。""",
        request = request,
    )

    suspend fun generateRepairPlan(config: ModelConfig, context: String, request: GenerationRequest? = null): Result<String> = chat(
        config = config,
        context = context,
        temperature = 0.3,
        systemInstruction = "你是中文小说责编。根据给出的章节和已发现的门禁问题，输出最短修复计划：按优先级列出具体要改的段落、修改目标和一个可直接采用的写法方向。不要重写全文，不要输出正文以外的空泛评价，不要建议提前揭露禁区。",
        request = request,
    )

    suspend fun extractStyleGuide(config: ModelConfig, sample: String, request: GenerationRequest? = null): Result<String> = chat(
        config = config,
        context = sample,
        temperature = 0.2,
        systemInstruction = "你是中文网文风格编辑。仅根据给出的样章，提取一个可执行的项目文风档案：叙事视角、时态、句长和节奏、对话比例、描写偏好、禁用表达、章末钩子习惯。用紧凑中文分点，不评价原文，不仿照在世作者，不输出小说正文。",
        request = request,
    )

    suspend fun generateProjectProfile(config: ModelConfig, context: String, request: GenerationRequest? = null): Result<String> = chat(
        config = config,
        context = context,
        temperature = 0.7,
        systemInstruction = """你是中文网文策划编辑。依据作者给出的书名和已有信息，补全可直接用于创作的作品设定。只输出一个合法 JSON 对象，不要 Markdown 或解释：
{
  "genre":"精确题材，最多12字",
  "premise":"一句话核心设定，最多60字",
  "summary":"120-180字的网文简介，突出爽点、冲突与悬念",
  "tags":"3-6个中文标签，用中文逗号分隔",
  "targetAudience":"目标读者描述，最多30字",
  "protagonistName":"主角姓名"
}
保留并强化作者已给出的信息，不要引用现实在世作者，不要编造具体平台或版权信息。""",
        request = request,
    )

    suspend fun generateCover(
        config: ModelConfig,
        prompt: String,
        request: GenerationRequest? = null,
        size: String = "1024x1536",
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            require(config.imageBaseUrl.startsWith("https://")) { "请先填写封面 AI 的 Base URL" }
            require(config.imageApiKey.isNotBlank()) { "请先填写封面 AI 的 API Key" }
            require(config.imageModel.isNotBlank()) { "请先填写封面 AI 的模型名称" }
            val connection = URL(config.imageBaseUrl.trimEnd('/') + "/images/generations").openConnection() as HttpURLConnection
            request?.connection = connection
            try {
                val body = JSONObject().apply {
                    put("model", config.imageModel)
                    put("prompt", prompt)
                    put("size", size)
                    put("response_format", "b64_json")
                }.toString()
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Authorization", "Bearer " + config.imageApiKey)
                connection.connectTimeout = 20_000
                connection.readTimeout = 120_000
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) error("封面接口返回 HTTP $status: ${response.take(180)}")
                val data = JSONObject(response).getJSONArray("data").getJSONObject(0)
                when {
                    data.has("b64_json") -> android.util.Base64.decode(data.getString("b64_json"), android.util.Base64.DEFAULT)
                    data.has("url") -> URL(data.getString("url")).openStream().use { it.readBytes() }
                    else -> error("封面接口没有返回图片数据")
                }
            } finally {
                if (request?.connection === connection) request.connection = null
                connection.disconnect()
            }
        }
    }

    suspend fun testImage(config: ModelConfig): Result<String> =
        generateCover(
            config = config,
            prompt = "A simple abstract Chinese web novel cover composition, no text, no logo, no watermark.",
            size = "1024x1024",
        ).map { "封面 AI 连接成功，图像端点可用" }

    private suspend fun chat(
        config: ModelConfig,
        context: String,
        temperature: Double,
        systemInstruction: String,
        request: GenerationRequest? = null,
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
            request?.connection = connection
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
                if (request?.connection === connection) request.connection = null
                connection.disconnect()
            }
        }
    }
}
