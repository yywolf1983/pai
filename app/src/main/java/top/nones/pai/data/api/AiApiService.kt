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
        .readTimeout(60, TimeUnit.SECONDS)
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
                json.put("temperature", 0.7)

                val mediaType = "application/json".toMediaType()
                val body = json.toString().toRequestBody(mediaType)
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

data class MessageRequest(
    val role: String,
    val content: String
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("role", role)
        json.put("content", content)
        return json
    }
}
