package top.nones.pai.data.api

import android.util.Log
import top.nones.pai.data.model.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var currentCall: okhttp3.Call? = null
    
    fun cancelCurrentRequest() {
        currentCall?.cancel()
        currentCall = null
    }

    suspend fun sendMessage(
        modelConfig: ModelConfig,
        messages: List<MessageRequest>
    ): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject()
                json.put("model", modelConfig.modelName)
                json.put("messages", JSONArray(messages.map { it.toJson() }))
                json.put("temperature", 0.1)
                json.put("top_p", 0.1)
                json.put("frequency_penalty", 0.5)
                json.put("presence_penalty", 0.0)

                Log.d("AiApiService", "Sending request with ${messages.size} messages")
                messages.forEachIndexed { index, msg ->
                    Log.d("AiApiService", "Message $index: role=${msg.role}, hasAttachments=${msg.attachments.isNotEmpty()}")
                }
                val jsonString = json.toString()
                Log.d("AiApiService", "Request body length: ${jsonString.length}")

                val mediaType = "application/json".toMediaType()
                val body = jsonString.toRequestBody(mediaType)
                val normalizedEndpoint = normalizeEndpoint(modelConfig.endpoint)

                val builder = Request.Builder()
                    .url(normalizedEndpoint)
                    .addHeader("Content-Type", "application/json")
                if (modelConfig.apiKey.isNotBlank()) {
                    builder.addHeader("Authorization", "Bearer ${modelConfig.apiKey}")
                }
                val request = builder.post(body).build()
                currentCall = client.newCall(request)

                val response = currentCall?.execute()
                if (response == null) {
                    return@withContext ApiResult.Error("请求已取消")
                }
                response.use {
                    val responseBody = it.body?.string()
                    if (!it.isSuccessful) {
                        val bodySummary = responseBody?.take(280) ?: "empty body"
                        return@withContext ApiResult.Error("HTTP ${it.code}: $bodySummary")
                    }

                    if (responseBody == null) {
                        return@withContext ApiResult.Error("响应为空")
                    }

                    val responseJson = JSONObject(responseBody)
                    val choices = responseJson.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val choice = choices.getJSONObject(0)
                        val message = choice.optJSONObject("message")
                        val content = message?.optString("content").orEmpty()
                        if (content.isNotBlank()) {
                            return@withContext ApiResult.Success(content)
                        }
                    }
                    return@withContext ApiResult.Error("模型返回格式异常，缺少内容")
                }
            } catch (e: IOException) {
                Log.e("AiApiService", "Network Exception: ${e.message}")
                return@withContext ApiResult.Error("网络错误: ${e.message}")
            } catch (e: Exception) {
                Log.e("AiApiService", "Exception: ${e.message}")
                return@withContext ApiResult.Error("请求异常: ${e.message}")
            }
        }
    }

    suspend fun sendMessageStream(
        modelConfig: ModelConfig,
        messages: List<MessageRequest>,
        onChunk: (String, String?) -> Unit, // (contentChunk, thinkingChunk)
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject()
                json.put("model", modelConfig.modelName)
                json.put("messages", JSONArray(messages.map { it.toJson() }))
                json.put("temperature", 0.1)
                json.put("top_p", 0.1)
                json.put("frequency_penalty", 0.5)
                json.put("presence_penalty", 0.0)
                json.put("stream", true)

                Log.d("AiApiService", "Sending stream request with ${messages.size} messages")
                messages.forEachIndexed { index, msg ->
                    Log.d("AiApiService", "Stream Message $index: role=${msg.role}, hasAttachments=${msg.attachments.isNotEmpty()}")
                }
                val jsonString = json.toString()
                Log.d("AiApiService", "Stream request body length: ${jsonString.length}")

                val mediaType = "application/json".toMediaType()
                val body = jsonString.toRequestBody(mediaType)
                val normalizedEndpoint = normalizeEndpoint(modelConfig.endpoint)

                val builder = Request.Builder()
                    .url(normalizedEndpoint)
                    .addHeader("Content-Type", "application/json")
                if (modelConfig.apiKey.isNotBlank()) {
                    builder.addHeader("Authorization", "Bearer ${modelConfig.apiKey}")
                }
                val request = builder.post(body).build()
                currentCall = client.newCall(request)

                val response = currentCall?.execute()
                if (response == null) {
                    onError("请求已取消")
                    return@withContext
                }
                response.use {
                    if (!it.isSuccessful) {
                        val errorBody = it.body?.string()
                        val errorMessage = "HTTP ${it.code}: ${errorBody?.take(280) ?: "empty body"}"
                        onError(errorMessage)
                        return@withContext
                    }

                    val source = it.body?.source() ?: run {
                        onError("响应为空")
                        return@withContext
                    }

                    while (!source.exhausted() && !currentCall?.isCanceled()!!) {
                        val line = source.readUtf8Line() ?: break
                        if (line.isBlank() || line == "data: [DONE]") {
                            if (line == "data: [DONE]") {
                                onComplete()
                            }
                            continue
                        }

                        if (line.startsWith("data: ")) {
                            val jsonStr = line.substring(6)
                            try {
                                Log.d("AiApiService", "Received chunk: ${jsonStr.take(200)}")
                                val json = JSONObject(jsonStr)
                                val choices = json.optJSONArray("choices")
                                if (choices != null && choices.length() > 0) {
                                    val choice = choices.getJSONObject(0)
                                    val delta = choice.optJSONObject("delta")
                                    val content = delta?.optString("content").orEmpty().takeIf { it != "null" } ?: ""
                                    
                                    // 支持多种思考字段名
                                    var thinking = ""
                                    if (delta != null) {
                                        val thinkingKeys = listOf("thinking", "reasoning", "thought", "think", "reasoning_content")
                                        for (key in thinkingKeys) {
                                            val value = delta.optString(key).orEmpty().takeIf { it != "null" } ?: ""
                                            if (value.isNotEmpty()) {
                                                thinking = value
                                                Log.d("AiApiService", "Found thinking content with key: $key")
                                                break
                                            }
                                        }
                                        
                                        // 检查是否在reasoning字段中
                                        if (thinking.isEmpty()) {
                                            val reasoningObj = delta.optJSONObject("reasoning")
                                            if (reasoningObj != null) {
                                                thinking = reasoningObj.optString("content").orEmpty().takeIf { it != "null" } ?: ""
                                            }
                                        }
                                    }
                                    
                                    if (content.isNotEmpty() || thinking.isNotEmpty()) {
                                        Log.d("AiApiService", "Content: ${content.take(50)}, Thinking: ${thinking.take(50)}")
                                        onChunk(content, if (thinking.isNotEmpty()) thinking else null)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("AiApiService", "Error parsing stream chunk: ${e.message}")
                            }
                        }
                    }

                    if (currentCall?.isCanceled() == true) {
                        onError("请求已取消")
                    }
                }
            } catch (e: IOException) {
                Log.e("AiApiService", "Stream Network Exception: ${e.message}")
                onError("网络错误: ${e.message}")
            } catch (e: Exception) {
                Log.e("AiApiService", "Stream Exception: ${e.message}")
                onError("请求异常: ${e.message}")
            }
        }
    }

    suspend fun testConnection(modelConfig: ModelConfig): ApiResult<String> {
        val pingMessages = listOf(
            MessageRequest(role = "user", content = "Reply with OK only.")
        )
        return when (val result = sendMessage(modelConfig, pingMessages)) {
            is ApiResult.Success -> ApiResult.Success("连接成功，模型返回: ${result.data.take(30)}")
            is ApiResult.Error -> ApiResult.Error(result.message)
        }
    }

    suspend fun fetchModels(modelConfig: ModelConfig): ApiResult<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val modelsUrl = normalizeModelsEndpoint(modelConfig.endpoint)
                val builder = Request.Builder().url(modelsUrl)
                if (modelConfig.apiKey.isNotBlank()) {
                    builder.addHeader("Authorization", "Bearer ${modelConfig.apiKey}")
                }
                val request = builder.get().build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful) {
                        val summary = body?.take(280) ?: "empty body"
                        return@withContext ApiResult.Error("拉取模型失败 HTTP ${response.code}: $summary")
                    }
                    if (body.isNullOrBlank()) {
                        return@withContext ApiResult.Error("模型列表响应为空")
                    }
                    val json = JSONObject(body)
                    val models = mutableListOf<String>()
                    val openAiData = json.optJSONArray("data")
                    if (openAiData != null) {
                        for (i in 0 until openAiData.length()) {
                            val id = openAiData.optJSONObject(i)?.optString("id").orEmpty()
                            if (id.isNotBlank()) models += id
                        }
                    }
                    val ollamaModels = json.optJSONArray("models")
                    if (ollamaModels != null) {
                        for (i in 0 until ollamaModels.length()) {
                            val item = ollamaModels.optJSONObject(i)
                            val name = item?.optString("name").orEmpty()
                            if (name.isNotBlank()) models += name
                        }
                    }
                    val distinct = models.distinct()
                    if (distinct.isEmpty()) {
                        return@withContext ApiResult.Error("未解析到可用模型，请确认接口为 /v1/models 或兼容格式")
                    }
                    return@withContext ApiResult.Success(distinct)
                }
            } catch (e: IOException) {
                return@withContext ApiResult.Error("网络错误: ${e.message}")
            } catch (e: Exception) {
                return@withContext ApiResult.Error("拉取模型异常: ${e.message}")
            }
        }
    }

    private fun normalizeEndpoint(input: String): String {
        val trimmed = input.trim().removeSuffix("/")
        return if (trimmed.endsWith("/v1/chat/completions")) {
            trimmed
        } else {
            "$trimmed/v1/chat/completions"
        }
    }

    private fun normalizeModelsEndpoint(input: String): String {
        val trimmed = input.trim().removeSuffix("/")
        return when {
            trimmed.endsWith("/v1/chat/completions") -> trimmed.removeSuffix("/chat/completions") + "/models"
            trimmed.endsWith("/v1/models") -> trimmed
            else -> "$trimmed/v1/models"
        }
    }
}

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

sealed class ContentBlock {
    data class Text(val text: String) : ContentBlock()
    data class ImageUrl(val url: String, val detail: String = "auto") : ContentBlock()
    data class Audio(val url: String) : ContentBlock()
}

data class MessageRequest(
    val role: String,
    val content: String,
    val attachments: List<AttachmentRequest> = emptyList()
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("role", role)
        
        if (attachments.isEmpty()) {
            json.put("content", content)
        } else {
            val contentArray = JSONArray()
            
            if (content.isNotEmpty()) {
                val textObj = JSONObject()
                textObj.put("type", "text")
                textObj.put("text", content)
                contentArray.put(textObj)
            }
            
            attachments.forEach { attachment ->
                when (attachment.type) {
                    "image" -> {
                        val imageObj = JSONObject()
                        imageObj.put("type", "image_url")
                        val imageUrlObj = JSONObject()
                        imageUrlObj.put("url", "data:${attachment.mimeType};base64,${attachment.base64Data}")
                        imageUrlObj.put("detail", attachment.detail ?: "auto")
                        imageObj.put("image_url", imageUrlObj)
                        contentArray.put(imageObj)
                    }
                    "audio" -> {
                        val audioObj = JSONObject()
                        audioObj.put("type", "audio")
                        audioObj.put("audio", JSONObject().apply {
                            put("url", "data:${attachment.mimeType};base64,${attachment.base64Data}")
                        })
                        contentArray.put(audioObj)
                    }
                    "text" -> {
                        val textObj = JSONObject()
                        textObj.put("type", "text")
                        textObj.put("text", attachment.content ?: "")
                        contentArray.put(textObj)
                    }
                }
            }
            
            json.put("content", contentArray)
        }
        return json
    }
}

data class AttachmentRequest(
    val type: String,
    val mimeType: String,
    val base64Data: String,
    val detail: String? = null,
    val content: String? = null
)
