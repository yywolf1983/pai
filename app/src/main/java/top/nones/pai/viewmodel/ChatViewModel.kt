package top.nones.pai.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.nones.pai.data.api.AiApiService
import top.nones.pai.data.api.ApiResult
import top.nones.pai.data.api.AttachmentRequest
import top.nones.pai.data.api.MessageRequest
import top.nones.pai.data.model.Attachment
import top.nones.pai.data.model.ChatSession
import top.nones.pai.data.model.Message
import top.nones.pai.data.model.ModelConfig
import top.nones.pai.utils.DirectoryBindingManager
import top.nones.pai.utils.FileOperationParser
import top.nones.pai.utils.FileUtils
import top.nones.pai.utils.SecurityManager

class ChatViewModel : ViewModel() {
    private val aiApiService = AiApiService()
    private var currentChatId: Long? = null
    
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

    private val _streamingContent = MutableStateFlow<String>("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    private val _thinkingContent = MutableStateFlow<String?>(null)
    val thinkingContent: StateFlow<String?> = _thinkingContent.asStateFlow()

    private val _outputSpeed = MutableStateFlow<Double>(0.0)
    val outputSpeed: StateFlow<Double> = _outputSpeed.asStateFlow()

    private val _outputSize = MutableStateFlow<Int>(0)
    val outputSize: StateFlow<Int> = _outputSize.asStateFlow()

    private val _contextSize = MutableStateFlow<Int>(0)
    val contextSize: StateFlow<Int> = _contextSize.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private var startTime = 0L
    private var totalChars = 0

    private val _modelTestStatus = MutableStateFlow<String?>(null)
    val modelTestStatus: StateFlow<String?> = _modelTestStatus.asStateFlow()
    private val _isTestingModel = MutableStateFlow(false)
    val isTestingModel: StateFlow<Boolean> = _isTestingModel.asStateFlow()
    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()
    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels: StateFlow<Boolean> = _isFetchingModels.asStateFlow()

    fun bootstrap(context: Context) {
        loadFromSharedPreferences(context)
        publishAll()
        setupDefaultDirectory(context)
    }

    private fun setupDefaultDirectory(context: Context) {
        val bindingManager = DirectoryBindingManager(context)
        if (!bindingManager.isBound()) {
            val possibleDirs = listOf(
                "/storage/emulated/0/Documents",
                context.getExternalFilesDir(null)?.absolutePath,
                context.filesDir.absolutePath
            )
            
            for (dirPath in possibleDirs) {
                if (dirPath == null) continue
                try {
                    val dirFile = java.io.File(dirPath)
                    if (!dirFile.exists()) {
                        dirFile.mkdirs()
                    }
                    if (dirFile.exists() && dirFile.isDirectory) {
                        bindingManager.bindDirectory(dirPath)
                        return
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        }
    }

    fun loadChatSessions() {
        _chatSessions.value = localChats.sortedByDescending { it.updatedAtMillis }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadMessages(chatId: Long) {
        currentChatId = chatId
        _messages.value = localMessages.filter { it.chatId == chatId }.sortedBy { it.timestampMillis }
    }

    fun loadModelConfigs() {
        _modelConfigs.value = localConfigs.sortedWith(compareByDescending<ModelConfig> { it.isDefault }.thenBy { it.name })
    }

    fun sendMessage(context: Context, content: String, attachments: List<Attachment> = emptyList()) {
        val chatId = currentChatId ?: return
        val trimmed = content.trim()
        if (trimmed.isEmpty() && attachments.isEmpty()) return

        val attachmentRequests = processAttachments(context, attachments)
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
                    _isStreaming.value = true
                    _aiStatus.value = "正在发送请求..."
                    _streamingContent.value = ""
                    _thinkingContent.value = null
                    _outputSpeed.value = 0.0
                    _outputSize.value = 0

                    val history = localMessages.filter { it.chatId == chatId }.sortedBy { it.timestampMillis }
                    val toolDesc = FileOperationParser().getToolDescription()
                    val requestMessages = buildRequestMessagesWithTools(modelConfig, history, trimmed, toolDesc, attachmentRequests)
                    
                    val contextSize = requestMessages.sumOf { it.content.length }
                    _contextSize.value = contextSize
                    
                    startTime = System.currentTimeMillis()
                    totalChars = 0
                    
                    try {
                        _aiStatus.value = "正在处理响应..."
                        var contentBuffer = ""
                        var thinkingBuffer = ""
                        var lastUpdateTime = System.currentTimeMillis()
                        val UPDATE_INTERVAL = 100L
                        
                        aiApiService.sendMessageStream(
                            modelConfig = modelConfig,
                            messages = requestMessages,
                            onChunk = { contentChunk: String, thinkingChunk: String? ->
                                val currentTime = System.currentTimeMillis()
                                var needsUpdate = false
                                
                                if (thinkingChunk != null) {
                                    thinkingBuffer += thinkingChunk
                                    totalChars += thinkingChunk.length
                                    needsUpdate = true
                                }
                                if (contentChunk.isNotEmpty()) {
                                    contentBuffer += contentChunk
                                    totalChars += contentChunk.length
                                    _outputSize.value = totalChars
                                    needsUpdate = true
                                }
                                
                                if (needsUpdate) {
                                    val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                                    if (elapsedTime > 0) {
                                        _outputSpeed.value = totalChars / elapsedTime
                                    }
                                }
                                
                                if (needsUpdate && (currentTime - lastUpdateTime >= UPDATE_INTERVAL)) {
                                    _thinkingContent.value = thinkingBuffer
                                    _streamingContent.value = contentBuffer
                                    lastUpdateTime = currentTime
                                }
                            },
                            onComplete = {
                                _aiStatus.value = "处理完成"
                                _thinkingContent.value = thinkingBuffer
                                _streamingContent.value = contentBuffer
                                val finalContent = contentBuffer
                                if (finalContent.isNotEmpty()) {
                                    val parser = FileOperationParser()
                                    val operation = parser.parseCommand(finalContent)
                                    val isToolCall = operation.type != FileOperationParser.OperationType.UNKNOWN
                                    val isPureToolCall = isToolCall && finalContent.trim().startsWith("{") && finalContent.trim().endsWith("}")
                                    val userHasFileIntent = containsFileOperationIntent(trimmed)

                                    if (!isPureToolCall) {
                                        val aiMessage = Message(
                                            id = nextMessageId(),
                                            chatId = chatId,
                                            content = finalContent,
                                            role = "assistant"
                                        )
                                        localMessages += aiMessage
                                    }

                                    saveToSharedPreferences(context)
                                    _messages.value = localMessages.filter { it.chatId == chatId }.sortedBy { it.timestampMillis }

                                    val index = localChats.indexOfFirst { it.id == chatId }
                                    if (index >= 0) {
                                        localChats[index] = localChats[index].copy(
                                            updatedAtMillis = System.currentTimeMillis(),
                                            title = if (localChats[index].title.isBlank() || localChats[index].title == "新聊天") trimmed.take(20) else localChats[index].title
                                        )
                                        saveToSharedPreferences(context)
                                        loadChatSessions()
                                    }

                                    if (isToolCall && userHasFileIntent) {
                                        val toolResult = handleFileOperation(context, chatId, operation)
                                        val toolMessage = Message(
                                            id = nextMessageId(),
                                            chatId = chatId,
                                            content = toolResult,
                                            role = "assistant"
                                        )
                                        localMessages += toolMessage
                                        saveToSharedPreferences(context)
                                        _messages.value = localMessages.filter { it.chatId == chatId }.sortedBy { it.timestampMillis }
                                    }
                                }
                                viewModelScope.launch {
                                    delay(2000)
                                    _aiStatus.value = null
                                    _thinkingContent.value = null
                                    _isStreaming.value = false
                                }
                            },
                            onError = { errorMessage: String ->
                                _aiStatus.value = "处理失败"
                                _thinkingContent.value = thinkingBuffer
                                _streamingContent.value = contentBuffer
                                _errorMessage.value = "AI 聊天失败: $errorMessage"
                                viewModelScope.launch {
                                    delay(2000)
                                    _aiStatus.value = null
                                    _thinkingContent.value = null
                                    _isStreaming.value = false
                                }
                            }
                        )
                    } catch (e: Exception) {
                        _aiStatus.value = "处理异常"
                        _errorMessage.value = "发送消息失败: ${e.message}"
                        _isStreaming.value = false
                        viewModelScope.launch {
                            delay(2000)
                            _aiStatus.value = null
                            _thinkingContent.value = null
                        }
                    } finally {
                        _isLoading.value = false
                    }
                } else {
                    _errorMessage.value = "未找到模型配置"
                }
            } catch (e: Exception) {
                _errorMessage.value = "发送消息失败: ${e.message}"
                _isLoading.value = false
                _isStreaming.value = false
            }
        }
    }

    private fun handleFileOperation(context: Context, chatId: Long, operation: FileOperationParser.FileOperation): String {
        return try {
            _isLoading.value = true
            
            val result = when (operation.type) {
                FileOperationParser.OperationType.BIND_DIRECTORY -> {
                    _aiStatus.value = "正在绑定目录..."
                    val manager = DirectoryBindingManager(context)
                    if (manager.bindDirectory(operation.filePath)) {
                        "目录绑定成功: ${operation.filePath}\n\n现在您可以使用自然语言指令在该目录下进行文件操作，例如：\n- 读取笔记.txt\n- 创建文件 report.md\n- 整理文件"
                    } else {
                        "目录绑定失败：该目录不允许访问\n\n请选择以下目录之一：\n- /sdcard/Download\n- /sdcard/Documents\n- /sdcard/Pictures\n- /sdcard/Music\n- /sdcard/DCIM"
                    }
                }
                FileOperationParser.OperationType.UNBIND_DIRECTORY -> {
                    _aiStatus.value = "正在取消绑定..."
                    DirectoryBindingManager(context).unbind()
                    "已取消目录绑定，将使用应用默认目录"
                }
                FileOperationParser.OperationType.SHOW_BOUND_DIRECTORY -> {
                    _aiStatus.value = "查询绑定目录..."
                    val manager = DirectoryBindingManager(context)
                    val path = manager.getBoundDirectory()
                    if (path != null) {
                        "当前绑定目录: $path\n\n您可以使用以下命令管理目录：\n- 绑定目录到 /新路径\n- 取消目录绑定"
                    } else {
                        "尚未绑定目录，请使用 \"绑定目录到 /path/to/dir\" 命令设置工作目录"
                    }
                }
                else -> {
                    if (!SecurityManager.checkOperationSafety(context, operation)) {
                        _aiStatus.value = "操作失败"
                        "安全检查失败：操作路径不在安全目录内\n\n提示：请先使用 \"绑定目录到 /path/to/dir\" 设置工作目录"
                    } else {
                        _aiStatus.value = "正在执行文件操作..."
                        val fileName = if (operation.filePath.startsWith("/")) java.io.File(operation.filePath).name else operation.filePath
                        
                        when (operation.type) {
                            FileOperationParser.OperationType.READ -> {
                                try {
                                    val content = FileUtils.readFile(context, operation.filePath)
                                    "已读取文件: $fileName\n\n文件内容:\n$content"
                                } catch (e: Exception) {
                                    "读取文件失败: $fileName\n错误: ${e.message}"
                                }
                            }
                            FileOperationParser.OperationType.WRITE -> {
                                val success = FileUtils.writeFile(context, operation.filePath, operation.content)
                                if (success) "文件写入成功: $fileName" else "文件写入失败: $fileName"
                            }
                            FileOperationParser.OperationType.APPEND -> {
                                val success = FileUtils.appendToFile(context, operation.filePath, operation.content)
                                if (success) "内容追加成功: $fileName" else "追加失败: $fileName"
                            }
                            FileOperationParser.OperationType.CREATE -> {
                                val success = FileUtils.createFile(context, operation.filePath)
                                if (success) "文件创建成功: $fileName" else "文件创建失败: $fileName"
                            }
                            FileOperationParser.OperationType.DELETE -> {
                                val success = FileUtils.deleteFile(context, operation.filePath)
                                if (success) "文件删除成功: $fileName" else "文件删除失败: $fileName"
                            }
                            FileOperationParser.OperationType.LIST -> {
                                val files = FileUtils.listFiles(context, operation.filePath)
                                if (files.isEmpty()) "目录为空" else "目录内容:\n${files.joinToString("\n")}"
                            }
                            FileOperationParser.OperationType.EXIST -> {
                                val exists = FileUtils.fileExists(context, operation.filePath)
                                if (exists) "文件存在: $fileName" else "文件不存在: $fileName"
                            }
                            FileOperationParser.OperationType.SIZE -> {
                                val size = FileUtils.getFileSize(context, operation.filePath)
                                "文件大小: $fileName\n大小: $size 字节"
                            }
                            FileOperationParser.OperationType.RENAME -> {
                                val success = FileUtils.renameFile(context, operation.filePath, operation.content)
                                if (success) "文件重命名成功: $fileName -> ${operation.content}" else "文件重命名失败: $fileName"
                            }
                            FileOperationParser.OperationType.MOVE -> {
                                val targetName = if (operation.content.startsWith("/")) java.io.File(operation.content).name else operation.content
                                val success = FileUtils.moveFile(context, operation.filePath, operation.content)
                                if (success) "文件移动成功: $fileName -> $targetName" else "文件移动失败: $fileName"
                            }
                            FileOperationParser.OperationType.COPY -> {
                                val targetName = if (operation.content.startsWith("/")) java.io.File(operation.content).name else operation.content
                                val success = FileUtils.copyFile(context, operation.filePath, operation.content)
                                if (success) "文件复制成功: $fileName -> $targetName" else "文件复制失败: $fileName"
                            }
                            FileOperationParser.OperationType.BATCH_DELETE -> {
                                val count = FileUtils.batchDeleteFiles(context, operation.filePath)
                                "批量删除完成，共删除 $count 个文件"
                            }
                            FileOperationParser.OperationType.BATCH_RENAME -> {
                                val prefix = operation.options.getOrDefault("prefix", "")
                                val count = FileUtils.batchRenameFiles(context, operation.filePath, prefix)
                                "批量重命名完成，共重命名 $count 个文件"
                            }
                            FileOperationParser.OperationType.BATCH_MOVE -> {
                                val count = FileUtils.batchMoveFiles(context, operation.filePath, operation.content)
                                "批量移动完成，共移动 $count 个文件"
                            }
                            FileOperationParser.OperationType.SMART_SORT -> {
                                val sortBy = operation.options.getOrDefault("sort_by", "type")
                                val target = operation.options.getOrDefault("target", operation.filePath)
                                val count = FileUtils.smartSortFiles(context, operation.filePath, sortBy, target)
                                "智能整理完成，共整理 $count 个文件"
                            }
                            else -> "未知操作"
                        }
                    }
                }
            }
            
            _aiStatus.value = "操作完成"
            result
        } catch (e: Exception) {
            _aiStatus.value = "操作失败"
            _errorMessage.value = "文件操作失败: ${e.message}"
            "文件操作失败: ${e.message}"
        } finally {
            _isLoading.value = false
            viewModelScope.launch {
                delay(2000)
                _aiStatus.value = null
            }
        }
    }

    private fun processAttachments(context: Context, attachments: List<Attachment>): List<AttachmentRequest> {
        if (attachments.isEmpty()) return emptyList()

        val attachmentRequests = mutableListOf<AttachmentRequest>()

        attachments.forEachIndexed { index, attachment ->
            try {
                val mimeType = FileUtils.getMimeType(attachment.name)
                val attachmentType = FileUtils.getAttachmentType(mimeType)

                when (attachmentType) {
                    "image", "audio" -> {
                        val base64Data = FileUtils.fileToBase64(context, attachment.path)
                        if (base64Data != null) {
                            attachmentRequests.add(
                                AttachmentRequest(
                                    type = attachmentType,
                                    mimeType = mimeType,
                                    base64Data = base64Data,
                                    detail = if (attachmentType == "image") "auto" else null
                                )
                            )
                        }
                    }
                    "text" -> {
                        val content = FileUtils.readUriOrFile(context, attachment.path)
                        attachmentRequests.add(
                            AttachmentRequest(
                                type = "text",
                                mimeType = mimeType,
                                base64Data = "",
                                content = content
                            )
                        )
                    }
                    else -> {
                        val content = FileUtils.readUriOrFile(context, attachment.path)
                        attachmentRequests.add(
                            AttachmentRequest(
                                type = "text",
                                mimeType = mimeType,
                                base64Data = "",
                                content = "[文件: ${attachment.name}]\n$content"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Skip failed attachments
            }
        }

        return attachmentRequests
    }

    private fun readAttachmentContent(context: Context, attachment: Attachment): String {
        return FileUtils.readFile(context, attachment.name)
    }

    fun createNewChat(
        context: Context,
        modelId: Long? = null,
        title: String = "新聊天",
        callback: ((Long) -> Unit)? = null
    ) {
        val newChat = ChatSession(
            id = nextChatId(),
            title = title,
            modelId = modelId ?: localConfigs.firstOrNull { it.isDefault }?.id ?: 0L,
            createdAtMillis = System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis()
        )
        
        localChats.add(newChat)
        saveToSharedPreferences(context)
        loadChatSessions()
        loadMessages(newChat.id)
        callback?.invoke(newChat.id)
    }

    fun deleteChat(context: Context, chatSession: ChatSession) {
        localChats.removeIf { it.id == chatSession.id }
        localMessages.removeAll { it.chatId == chatSession.id }
        saveToSharedPreferences(context)
        loadChatSessions()
        
        if (currentChatId == chatSession.id) {
            currentChatId = localChats.lastOrNull()?.id
            loadMessages(currentChatId ?: return)
        }
    }

    fun renameChat(context: Context, chatId: Long, newTitle: String) {
        val index = localChats.indexOfFirst { it.id == chatId }
        if (index >= 0) {
            localChats[index] = localChats[index].copy(
                title = newTitle,
                updatedAtMillis = System.currentTimeMillis()
            )
            saveToSharedPreferences(context)
            loadChatSessions()
        }
    }

    fun createModelConfig(context: Context, modelConfig: ModelConfig) {
        try {
            if (modelConfig.isDefault) {
                localConfigs.replaceAll { it.copy(isDefault = false) }
            }
            if (modelConfig.id == 0L) {
                val newId = nextConfigId()
                localConfigs += modelConfig.copy(id = newId)
            } else {
                val index = localConfigs.indexOfFirst { it.id == modelConfig.id }
                if (index >= 0) {
                    localConfigs[index] = modelConfig
                } else {
                    localConfigs += modelConfig
                }
            }
            saveToSharedPreferences(context)
            loadModelConfigs()
        } catch (e: Exception) {
            _errorMessage.value = "创建模型配置失败: ${e.message}"
        }
    }

    fun setDefaultModel(context: Context, modelConfig: ModelConfig) {
        try {
            localConfigs.replaceAll { it.copy(isDefault = it.id == modelConfig.id) }
            saveToSharedPreferences(context)
            loadModelConfigs()
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
            loadModelConfigs()
        } catch (e: Exception) {
            _errorMessage.value = "删除模型配置失败: ${e.message}"
        }
    }

    fun deleteMessage(context: Context, message: Message) {
        localMessages.removeAll { it.id == message.id }
        saveToSharedPreferences(context)
        loadMessages(message.chatId)
    }

    fun clearModelTestStatus() {
        _isLoading.value = false
        _isStreaming.value = false
        _aiStatus.value = null
        _streamingContent.value = ""
        _thinkingContent.value = null
        _errorMessage.value = null
    }

    fun clearAvailableModels() {
        _modelConfigs.value = emptyList()
    }

    fun testModelConfig(config: ModelConfig) {
        viewModelScope.launch {
            _isTestingModel.value = true
            try {
                when (val result = aiApiService.testConnection(config)) {
                    is ApiResult.Success<*> -> _modelTestStatus.value = result.data as String
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
                    is ApiResult.Success<*> -> {
                        _availableModels.value = result.data as List<String>
                        _modelTestStatus.value = "已获取 ${(result.data as List<*>).size} 个模型"
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

    fun cancelAiRequest() {
        _isStreaming.value = false
        _isLoading.value = false
        _aiStatus.value = "已取消"
    }

    private fun buildRequestMessagesWithTools(
        config: ModelConfig,
        history: List<Message>,
        currentContent: String?,
        toolDescription: String,
        attachmentRequests: List<AttachmentRequest> = emptyList()
    ): List<MessageRequest> {
        val result = mutableListOf<MessageRequest>()

        val hasFileIntent = currentContent?.let { containsFileOperationIntent(it) } ?: false
        
        val systemPrompt = if (config.systemPrompt.isNotBlank()) {
            if (hasFileIntent) {
                "${config.systemPrompt}\n\n你可以使用以下工具来操作文件：\n$toolDescription"
            } else {
                config.systemPrompt
            }
        } else {
            if (hasFileIntent) {
                "你可以使用以下工具来操作文件：\n$toolDescription"
            } else {
                ""
            }
        }
        if (systemPrompt.isNotEmpty()) {
            result += MessageRequest(role = "system", content = systemPrompt)
        }

        if (currentContent != null && history.isNotEmpty()) {
            result += history.subList(0, history.size - 1).map { message ->
                var content = message.content
                if (message.attachments.isNotEmpty()) {
                    val attachmentInfo = message.attachments.joinToString("\n") { "- ${it.name} (${it.type})" }
                    content = "$content\n\n附件:\n$attachmentInfo"
                }
                MessageRequest(role = message.role, content = content)
            }
            val lastMessage = history.last()
            result += MessageRequest(
                role = lastMessage.role,
                content = currentContent,
                attachments = attachmentRequests
            )
        } else {
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

    private fun buildRequestMessages(
        config: ModelConfig,
        history: List<Message>,
        processedContent: String? = null
    ): List<MessageRequest> {
        val result = mutableListOf<MessageRequest>()
        if (config.systemPrompt.isNotBlank()) {
            result += MessageRequest(role = "system", content = config.systemPrompt)
        }
        
        if (processedContent != null && history.isNotEmpty()) {
            result += history.subList(0, history.size - 1).map { message ->
                var content = message.content
                if (message.attachments.isNotEmpty()) {
                    val attachmentInfo = message.attachments.joinToString("\n") { "- ${it.name} (${it.type})" }
                    content = "$content\n\n附件:\n$attachmentInfo"
                }
                MessageRequest(role = message.role, content = content)
            }
            val lastMessage = history.last()
            result += MessageRequest(role = lastMessage.role, content = processedContent)
        } else {
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
    
    private fun containsFileOperationIntent(content: String): Boolean {
        val fileIntentPatterns = listOf(
            "创建文件", "新建文件", "写文件", "写入文件", "保存文件",
            "读取文件", "读文件", "查看文件", "打开文件",
            "删除文件", "移除文件",
            "修改文件", "编辑文件",
            "重命名文件", "文件重命名",
            "移动文件", "复制文件",
            "列出文件", "文件列表", "查看目录", "列出目录",
            "帮我操作文件", "帮我创建", "帮我删除", "帮我修改",
            "帮我写", "帮我读", "帮我打开",
            "操作文件", "管理文件",
            "bind_directory", "write_file", "read_file", "delete_file", 
            "create_file", "append_to_file", "rename_file", "move_file", "copy_file"
        )
        val lowerContent = content.lowercase()
        return fileIntentPatterns.any { lowerContent.contains(it.lowercase()) }
    }
    
    private fun loadFromSharedPreferences(context: Context) {
        try {
            val prefs = context.getSharedPreferences("pai_app", Context.MODE_PRIVATE)
            
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
            
            val chatSessionsJson = prefs.getString(CHAT_SESSIONS_KEY, "")
            if (!chatSessionsJson.isNullOrEmpty()) {
                val type = object : TypeToken<List<ChatSession>>() {}.type
                val loadedChats = gson.fromJson<List<ChatSession>>(chatSessionsJson, type)
                localChats.clear()
                localChats.addAll(loadedChats)
                if (localChats.isNotEmpty()) {
                    chatIdSeed = localChats.maxByOrNull { it.id }?.id ?: 0
                }
            }
            
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
        } catch (e: Exception) {
            _errorMessage.value = "加载数据失败: ${e.message}"
        }
    }
    
    private fun saveToSharedPreferences(context: Context) {
        try {
            val prefs = context.getSharedPreferences("pai_app", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            val modelConfigsJson = gson.toJson(localConfigs)
            editor.putString(MODEL_CONFIGS_KEY, modelConfigsJson)
            
            val chatSessionsJson = gson.toJson(localChats)
            editor.putString(CHAT_SESSIONS_KEY, chatSessionsJson)
            
            val messagesJson = gson.toJson(localMessages)
            editor.putString(MESSAGES_KEY, messagesJson)
            
            editor.commit()
        } catch (e: Exception) {
        }
    }
}
