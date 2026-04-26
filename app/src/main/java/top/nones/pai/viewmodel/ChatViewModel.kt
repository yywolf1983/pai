package top.nones.pai.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import top.nones.pai.data.api.AiApiService
import top.nones.pai.data.api.ApiResult
import top.nones.pai.data.api.MessageRequest
import top.nones.pai.data.model.ChatSession
import top.nones.pai.data.model.Message
import top.nones.pai.data.model.ModelConfig
import top.nones.pai.data.model.Attachment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val aiApiService = AiApiService()
    private val localChats = mutableListOf<ChatSession>()
    private val localMessages = mutableListOf<Message>()
    private val localConfigs = mutableListOf<ModelConfig>()
    private var chatIdSeed = 0L
    private var messageIdSeed = 0L
    private var configIdSeed = 0L
    private val gson = Gson()
    private val MODEL_CONFIGS_KEY = "model_configs"
    private val CHAT_SESSIONS_KEY = "chat_sessions"
    private val MESSAGES_KEY = "messages"

    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _modelConfigs = MutableStateFlow<List<ModelConfig>>(emptyList())
    val modelConfigs: StateFlow<List<ModelConfig>> = _modelConfigs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _aiStatus = MutableStateFlow<String?>(null)
    val aiStatus: StateFlow<String?> = _aiStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val _modelTestStatus = MutableStateFlow<String?>(null)
    val modelTestStatus: StateFlow<String?> = _modelTestStatus.asStateFlow()
    private val _isTestingModel = MutableStateFlow(false)
    val isTestingModel: StateFlow<Boolean> = _isTestingModel.asStateFlow()
    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()
    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels: StateFlow<Boolean> = _isFetchingModels.asStateFlow()

    var currentChatId: Long? = null

    fun bootstrap(context: Context) {
        viewModelScope.launch {
            try {
                loadFromSharedPreferences(context)
                if (localConfigs.isEmpty()) {
                    localConfigs += ModelConfig(
                        id = nextConfigId(),
                        name = "本地 Ollama",
                        endpoint = "http://10.0.2.2:11434",
                        apiKey = "",
                        modelName = "qwen2.5:3b",
                        systemPrompt = "你是一个简洁可靠的手机 AI 助手。",
                        isLocal = true,
                        isDefault = true
                    )
                    saveToSharedPreferences(context)
                }
                publishAll()
            } catch (e: Exception) {
                _errorMessage.value = "初始化失败: ${e.message}"
            }
        }
    }

    fun loadChatSessions(context: Context) {
        _chatSessions.value = localChats.sortedByDescending { it.updatedAtMillis }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadMessages(context: Context, chatId: Long) {
        currentChatId = chatId
        _messages.value = localMessages.filter { it.chatId == chatId }.sortedBy { it.timestampMillis }
    }

    fun loadModelConfigs(context: Context) {
        _modelConfigs.value = localConfigs.sortedWith(compareByDescending<ModelConfig> { it.isDefault }.thenBy { it.name })
    }

    fun sendMessage(context: Context, content: String, attachments: List<Attachment> = emptyList()) {
        val chatId = currentChatId ?: return
        val trimmed = content.trim()
        if (trimmed.isEmpty() && attachments.isEmpty()) return
        
        // 处理附件，转换为 AI 可识别的格式
        val processedContent = processAttachments(context, trimmed, attachments)
        val userMessage = Message(chatId = chatId, content = trimmed, role = "user", attachments = attachments)

        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            try {
                localMessages += userMessage.copy(id = nextMessageId())

                val chatSession = localChats.firstOrNull { it.id == chatId }
                val modelConfig = localConfigs.firstOrNull { it.id == chatSession?.modelId }
                    ?: localConfigs.firstOrNull { it.isDefault }

                if (modelConfig != null) {
                    _isLoading.value = true
                    _aiStatus.value = "正在发送请求..."
                    val history = localMessages.filter { it.chatId == chatId }.sortedBy { it.timestampMillis }
                    try {
                        _aiStatus.value = "正在处理响应..."
                        when (val aiResponse = aiApiService.sendMessage(modelConfig, buildRequestMessages(modelConfig, history, processedContent))) {
                            is ApiResult.Success -> {
                                _aiStatus.value = "处理完成"
                                val aiMessage = Message(
                                    id = nextMessageId(),
                                    chatId = chatId,
                                    content = aiResponse.data,
                                    role = "assistant"
                                )

                                localMessages += aiMessage
                                saveToSharedPreferences(context)
                                _messages.value = localMessages.filter { it.chatId == chatId }.sortedBy { it.timestampMillis }

                                val index = localChats.indexOfFirst { it.id == chatId }
                                if (index >= 0) {
                                    localChats[index] = localChats[index].copy(
                                        updatedAtMillis = System.currentTimeMillis(),
                                        title = localChats[index].title.ifBlank { trimmed.take(20) }
                                    )
                                    saveToSharedPreferences(context)
                                    loadChatSessions(context)
                                }
                            }
                            is ApiResult.Error -> {
                                _aiStatus.value = "处理失败"
                                _errorMessage.value = "AI 聊天失败: ${aiResponse.message}"
                            }
                        }
                    } catch (e: Exception) {
                        _aiStatus.value = "处理异常"
                        _errorMessage.value = "发送消息失败: ${e.message}"
                    } finally {
                        // 延迟清除状态，让用户有时间看到
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(2000)
                            _aiStatus.value = null
                        }
                    }
                } else {
                    _errorMessage.value = "未找到模型配置"
                }
            } catch (e: Exception) {
                _errorMessage.value = "发送消息失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun processAttachments(context: Context, originalContent: String, attachments: List<Attachment>): String {
        if (attachments.isEmpty()) return originalContent
        
        val processedContent = StringBuilder(originalContent)
        processedContent.append("\n\n附件内容:\n")
        
        attachments.forEach { attachment ->
            try {
                val content = readAttachmentContent(context, attachment)
                processedContent.append("\n=== ${attachment.name} (${attachment.type}) ===\n")
                processedContent.append(content)
                processedContent.append("\n")
            } catch (e: Exception) {
                processedContent.append("\n=== ${attachment.name} (${attachment.type}) ===\n")
                processedContent.append("无法读取附件内容: ${e.message}")
                processedContent.append("\n")
            }
        }
        
        return processedContent.toString()
    }
    
    private fun readAttachmentContent(context: Context, attachment: Attachment): String {
        val uri = android.net.Uri.parse(attachment.path)
        val contentResolver = context.contentResolver
        
        return contentResolver.openInputStream(uri)?.use {inputStream ->
            inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        } ?: "无法打开附件"
    }

    fun createNewChat(
        context: Context,
        modelId: Long,
        title: String = "新聊天",
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val chatSession = ChatSession(
                    id = nextChatId(),
                    title = title,
                    modelId = modelId
                )
                localChats += chatSession
                saveToSharedPreferences(context)
                val chatId = chatSession.id
                currentChatId = chatId
                _messages.value = emptyList()
                loadChatSessions(context)
                onCreated(chatId)
            } catch (e: Exception) {
                _errorMessage.value = "创建聊天会话失败: ${e.message}"
            }
        }
    }

    fun deleteChat(context: Context, chatSession: ChatSession) {
        localMessages.removeAll { it.chatId == chatSession.id }
        localChats.removeAll { it.id == chatSession.id }
        saveToSharedPreferences(context)
        loadChatSessions(context)
        if (currentChatId == chatSession.id) {
            currentChatId = null
            _messages.value = emptyList()
        }
    }

    fun renameChat(context: Context, chatId: Long, newTitle: String) {
        val index = localChats.indexOfFirst { it.id == chatId }
        if (index >= 0) {
            localChats[index] = localChats[index].copy(title = newTitle)
            saveToSharedPreferences(context)
            loadChatSessions(context)
        }
    }

    fun createModelConfig(context: Context, modelConfig: ModelConfig) {
        try {
            if (modelConfig.isDefault) {
                localConfigs.replaceAll { it.copy(isDefault = false) }
            }
            if (modelConfig.id == 0L) {
                localConfigs += modelConfig.copy(id = nextConfigId())
            } else {
                val index = localConfigs.indexOfFirst { it.id == modelConfig.id }
                if (index >= 0) localConfigs[index] = modelConfig
            }
            saveToSharedPreferences(context)
            loadModelConfigs(context)
        } catch (e: Exception) {
            _errorMessage.value = "创建模型配置失败: ${e.message}"
        }
    }

    fun setDefaultModel(context: Context, modelConfig: ModelConfig) {
        try {
            localConfigs.replaceAll { it.copy(isDefault = it.id == modelConfig.id) }
            saveToSharedPreferences(context)
            loadModelConfigs(context)
        } catch (e: Exception) {
            _errorMessage.value = "设置默认模型失败: ${e.message}"
        }
    }

    fun deleteModelConfig(context: Context, modelConfig: ModelConfig) {
        try {
            localConfigs.removeAll { it.id == modelConfig.id }
            if (localConfigs.isNotEmpty() && localConfigs.none { it.isDefault }) {
                localConfigs[0] = localConfigs[0].copy(isDefault = true)
            }
            saveToSharedPreferences(context)
            loadModelConfigs(context)
        } catch (e: Exception) {
            _errorMessage.value = "删除模型配置失败: ${e.message}"
        }
    }

    fun deleteMessage(context: Context, message: Message) {
        try {
            localMessages.removeAll { it.id == message.id }
            saveToSharedPreferences(context)
            if (currentChatId != null) {
                loadMessages(context, currentChatId!!)
            }
        } catch (e: Exception) {
            _errorMessage.value = "删除消息失败: ${e.message}"
        }
    }

    fun clearModelTestStatus() {
        _modelTestStatus.value = null
    }

    fun clearAvailableModels() {
        _availableModels.value = emptyList()
    }

    fun cancelAiRequest() {
        aiApiService.cancelCurrentRequest()
        _isLoading.value = false
        _aiStatus.value = "请求已取消"
        // 延迟清除状态，让用户有时间看到
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _aiStatus.value = null
        }
    }

    fun testModelConfig(config: ModelConfig) {
        viewModelScope.launch {
            _isTestingModel.value = true
            try {
                when (val result = aiApiService.testConnection(config)) {
                    is ApiResult.Success -> _modelTestStatus.value = result.data
                    is ApiResult.Error -> _modelTestStatus.value = "连接失败: ${result.message}"
                }
            } finally {
                _isTestingModel.value = false
            }
        }
    }

    fun fetchModels(config: ModelConfig) {
        viewModelScope.launch {
            _isFetchingModels.value = true
            try {
                when (val result = aiApiService.fetchModels(config)) {
                    is ApiResult.Success -> {
                        _availableModels.value = result.data
                        _modelTestStatus.value = "已获取 ${result.data.size} 个模型"
                    }
                    is ApiResult.Error -> {
                        _availableModels.value = emptyList()
                        _modelTestStatus.value = "获取失败: ${result.message}"
                    }
                }
            } finally {
                _isFetchingModels.value = false
            }
        }
    }

    private fun buildRequestMessages(
        config: ModelConfig,
        history: List<Message>,
        processedContent: String? = null
    ): List<MessageRequest> {
        val result = mutableListOf<MessageRequest>()
        if (config.systemPrompt.isNotBlank()) {
            result += MessageRequest(role = "system", content = config.systemPrompt)
        }
        
        // 如果有处理后的内容，只使用处理后的内容作为最后一条消息
        if (processedContent != null && history.isNotEmpty()) {
            // 添加历史消息（除了最后一条）
            result += history.subList(0, history.size - 1).map { message ->
                var content = message.content
                if (message.attachments.isNotEmpty()) {
                    val attachmentInfo = message.attachments.joinToString("\n") { "- ${it.name} (${it.type})" }
                    content = "$content\n\n附件:\n$attachmentInfo"
                }
                MessageRequest(role = message.role, content = content)
            }
            // 添加最后一条消息，使用处理后的内容
            val lastMessage = history.last()
            result += MessageRequest(role = lastMessage.role, content = processedContent)
        } else {
            // 没有处理后的内容，使用所有历史消息的原始内容
            result += history.map { message ->
                var content = message.content
                if (message.attachments.isNotEmpty()) {
                    val attachmentInfo = message.attachments.joinToString("\n") { "- ${it.name} (${it.type})" }
                    content = "$content\n\n附件:\n$attachmentInfo"
                }
                MessageRequest(role = message.role, content = content)
            }
        }
        
        return result
    }

    private fun publishAll() {
        _chatSessions.value = localChats.sortedByDescending { it.updatedAtMillis }
        _modelConfigs.value = localConfigs.sortedWith(compareByDescending<ModelConfig> { it.isDefault }.thenBy { it.name })
    }

    private fun nextChatId(): Long = ++chatIdSeed
    private fun nextMessageId(): Long = ++messageIdSeed
    private fun nextConfigId(): Long = ++configIdSeed
    
    private fun loadFromSharedPreferences(context: Context) {
        val prefs = context.getSharedPreferences("pai_app", Context.MODE_PRIVATE)
        
        // 加载模型配置
        val modelConfigsJson = prefs.getString(MODEL_CONFIGS_KEY, "")
        if (!modelConfigsJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<ModelConfig>>() {}.type
            val loadedConfigs = gson.fromJson<List<ModelConfig>>(modelConfigsJson, type)
            localConfigs.clear()
            localConfigs.addAll(loadedConfigs)
            if (localConfigs.isNotEmpty()) {
                configIdSeed = localConfigs.maxByOrNull { it.id }?.id ?: 0
            }
        }
        
        // 加载聊天会话
        val chatSessionsJson = prefs.getString(CHAT_SESSIONS_KEY, "")
        if (!chatSessionsJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<ChatSession>>() {}.type
            val loadedSessions = gson.fromJson<List<ChatSession>>(chatSessionsJson, type)
            localChats.clear()
            localChats.addAll(loadedSessions)
            if (localChats.isNotEmpty()) {
                chatIdSeed = localChats.maxByOrNull { it.id }?.id ?: 0
            }
        }
        
        // 加载消息
        val messagesJson = prefs.getString(MESSAGES_KEY, "")
        if (!messagesJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<Message>>() {}.type
            val loadedMessages = gson.fromJson<List<Message>>(messagesJson, type)
            localMessages.clear()
            localMessages.addAll(loadedMessages)
            if (localMessages.isNotEmpty()) {
                messageIdSeed = localMessages.maxByOrNull { it.id }?.id ?: 0
            }
        }
    }
    
    private fun saveToSharedPreferences(context: Context) {
        val prefs = context.getSharedPreferences("pai_app", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // 保存模型配置
        val modelConfigsJson = gson.toJson(localConfigs)
        editor.putString(MODEL_CONFIGS_KEY, modelConfigsJson)
        
        // 保存聊天会话
        val chatSessionsJson = gson.toJson(localChats)
        editor.putString(CHAT_SESSIONS_KEY, chatSessionsJson)
        
        // 保存消息
        val messagesJson = gson.toJson(localMessages)
        editor.putString(MESSAGES_KEY, messagesJson)
        
        editor.apply()
    }
}
