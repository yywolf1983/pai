package top.nones.pai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import top.nones.pai.data.model.Attachment
import top.nones.pai.data.model.ModelConfig
import top.nones.pai.ui.ChatListScreen
import top.nones.pai.ui.ChatScreen
import top.nones.pai.ui.ModelConfigEditScreen
import top.nones.pai.ui.ModelConfigScreen
import top.nones.pai.ui.theme.DemoTheme
import top.nones.pai.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    private val chatViewModel = ChatViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        viewModel = chatViewModel,
                        context = this
                    )
                }
            }
        }
    }
}

sealed class Screen {
    object ChatList : Screen()
    data class Chat(val chatId: Long) : Screen()
    object ModelConfigList : Screen()
    data class ModelConfigEdit(val modelConfig: ModelConfig? = null) : Screen()
}

@Composable
fun AppNavigation(
    viewModel: ChatViewModel,
    context: Context
) {
    var currentScreen by remember {
        mutableStateOf<Screen>(Screen.ChatList)
    }
    
    // 编辑消息状态
    var editingMessage by remember {
        mutableStateOf<top.nones.pai.data.model.Message?>(null)
    }
    
    // 选中的附件
    var selectedAttachments by remember {
        mutableStateOf<List<Attachment>>(emptyList())
    }
    
    // 文件选择启动器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(it)
                val fileName = contentResolver.query(it, null, null, null, null)?.use {cursor ->
                    val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        cursor.getString(nameIndex)
                    } else {
                        "attachment"
                    }
                } ?: "attachment"
                
                val attachment = Attachment(
                    name = fileName,
                    path = it.toString(),
                    type = mimeType ?: "application/octet-stream"
                )
                
                // 将附件添加到选中列表
                selectedAttachments = selectedAttachments + attachment
                
                // 显示选择成功的提示
                Toast.makeText(context, "已选择附件: $fileName", Toast.LENGTH_SHORT).show()
                
                // 现在需要将附件传递给 ChatViewModel，以便在发送消息时包含附件
                // 这里简化处理，直接在发送消息时添加附件
                // 实际应用中可能需要更复杂的附件管理逻辑
                
            } catch (e: Exception) {
                Toast.makeText(context, "选择附件失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // 权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，启动文件选择器
            try {
                // 启动文件选择器，支持所有文件类型
                filePickerLauncher.launch(arrayOf("*/*"))
            } catch (e: Exception) {
                Toast.makeText(context, "启动文件选择器失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "需要存储权限才能选择附件", Toast.LENGTH_SHORT).show()
        }
    }

    // 收集状态
    val chatSessions by viewModel.chatSessions.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val modelConfigs by viewModel.modelConfigs.collectAsState()

    // 加载数据
    LaunchedEffect(Unit) {
        viewModel.bootstrap(context)
    }

    // 当聊天会话或模型配置变化时，检查是否需要自动创建聊天
    LaunchedEffect(chatSessions, modelConfigs) {
        if (chatSessions.isNotEmpty()) {
            // 如果有聊天会话，默认显示第一个
            val firstChat = chatSessions.first()
            if (currentScreen !is Screen.Chat) {
                viewModel.loadMessages(context, firstChat.id)
                currentScreen = Screen.Chat(firstChat.id)
            }
        } else if (modelConfigs.isNotEmpty()) {
            // 如果没有聊天会话但有模型配置，自动创建一个
            val defaultModel = modelConfigs.firstOrNull { it.isDefault } ?: modelConfigs.firstOrNull()
            if (defaultModel != null) {
                viewModel.createNewChat(context, defaultModel.id) { chatId ->
                    viewModel.loadMessages(context, chatId)
                    currentScreen = Screen.Chat(chatId)
                }
            }
        }
    }
    val isLoading by viewModel.isLoading.collectAsState()
    val aiStatus by viewModel.aiStatus.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val modelTestStatus by viewModel.modelTestStatus.collectAsState()
    val isTestingModel by viewModel.isTestingModel.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isFetchingModels by viewModel.isFetchingModels.collectAsState()

    // 显示错误消息
    Box(modifier = Modifier.fillMaxSize()) {
        // 导航逻辑
        when (currentScreen) {
            is Screen.ChatList -> {
                ChatListScreen(
                    chatSessions = chatSessions,
                    onChatSessionClick = { chatSession ->
                        viewModel.loadMessages(context, chatSession.id)
                        currentScreen = Screen.Chat(chatSession.id)
                    },
                    onDeleteChatClick = { chatSession ->
                        viewModel.deleteChat(context, chatSession)
                    },
                    onModelConfigClick = {
                        currentScreen = Screen.ModelConfigList
                    }
                )
            }

            is Screen.Chat -> {
                // 由于currentScreen是一个具有自定义getter的属性，我们使用when表达式的分支变量来获取chatId
                val chatId = (currentScreen as Screen.Chat).chatId
                val currentChat = viewModel.chatSessions.value.find { it.id == chatId }
                val chatTitle = currentChat?.title ?: "聊天"
                
                ChatScreen(
                    messages = messages,
                    isSending = isLoading,
                    chatTitle = chatTitle,
                    editingMessage = editingMessage,
                    selectedAttachments = selectedAttachments,
                    onSendMessage = {
                        // 如果是编辑模式，先删除原消息，再发送新消息
                        val msgToEdit = editingMessage
                        if (msgToEdit != null) {
                            viewModel.deleteMessage(context, msgToEdit)
                            editingMessage = null
                        }
                        viewModel.sendMessage(context, it, selectedAttachments)
                        // 发送后清除选中的附件
                        selectedAttachments = emptyList()
                    },
                    onDeleteMessage = {
                        viewModel.deleteMessage(context, it)
                    },
                    onResendMessage = {
                        // 提取原始消息内容（移除错误前缀）
                        val originalContent = if (it.content.startsWith("错误:")) {
                            it.content.substringAfter("错误:").trim()
                        } else {
                            it.content
                        }
                        viewModel.sendMessage(context, originalContent, it.attachments)
                    },
                    onEditMessage = {
                        editingMessage = it
                    },
                    onRenameChat = {
                        viewModel.renameChat(context, chatId, it)
                    },
                    onCancelAiRequest = {
                            viewModel.cancelAiRequest()
                        },
                        onSelectAttachments = {
                            // 检查并请求存储权限
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                android.Manifest.permission.READ_MEDIA_IMAGES
                            } else {
                                android.Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            permissionLauncher.launch(permission)
                        },
                        onRemoveAttachment = { index ->
                            // 移除指定位置的附件
                            val newAttachments = selectedAttachments.toMutableList()
                            newAttachments.removeAt(index)
                            selectedAttachments = newAttachments
                        },
                        onNewChat = {
                            // 自动创建新聊天，使用默认模型
                            val defaultModel = modelConfigs.firstOrNull { it.isDefault } ?: modelConfigs.firstOrNull()
                            if (defaultModel != null) {
                                viewModel.createNewChat(context, defaultModel.id) { newChatId ->
                                    viewModel.loadMessages(context, newChatId)
                                    currentScreen = Screen.Chat(newChatId)
                                }
                            }
                        },
                        onBack = {
                            currentScreen = Screen.ChatList
                        },
                        aiStatus = aiStatus
                )
            }

            is Screen.ModelConfigList -> {
                ModelConfigScreen(
                    modelConfigs = modelConfigs,
                    onModelConfigClick = { modelConfig ->
                        viewModel.clearModelTestStatus()
                        viewModel.clearAvailableModels()
                        currentScreen = Screen.ModelConfigEdit(modelConfig)
                    },
                    onNewModelConfigClick = {
                        viewModel.clearModelTestStatus()
                        viewModel.clearAvailableModels()
                        currentScreen = Screen.ModelConfigEdit()
                    },
                    onDeleteModelConfigClick = { modelConfig ->
                        viewModel.deleteModelConfig(context, modelConfig)
                    },
                    onSetDefaultClick = { modelConfig ->
                        viewModel.setDefaultModel(context, modelConfig)
                    },
                    onBack = {
                        currentScreen = Screen.ChatList
                    }
                )
            }

            is Screen.ModelConfigEdit -> {
                val modelConfig = (currentScreen as Screen.ModelConfigEdit).modelConfig
                ModelConfigEditScreen(
                    modelConfig = modelConfig,
                    isTesting = isTestingModel,
                    isFetchingModels = isFetchingModels,
                    testStatus = modelTestStatus,
                    availableModels = availableModels,
                    onTest = { candidate ->
                        viewModel.testModelConfig(candidate)
                    },
                    onFetchModels = { candidate ->
                        viewModel.fetchModels(candidate)
                    },
                    onClearAvailableModels = {
                        viewModel.clearAvailableModels()
                    },
                    onClearTestStatus = {
                        viewModel.clearModelTestStatus()
                    },
                    onSave = {
                        viewModel.createModelConfig(context, it)
                        viewModel.clearModelTestStatus()
                        viewModel.clearAvailableModels()
                        // 重新加载模型配置
                        viewModel.loadModelConfigs(context)
                        currentScreen = Screen.ModelConfigList
                    },
                    onCancel = {
                        viewModel.clearModelTestStatus()
                        viewModel.clearAvailableModels()
                        currentScreen = Screen.ModelConfigList
                    }
                )
            }
        }



        // 显示错误消息
        errorMessage?.let {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text(text = "错误") },
                text = { Text(text = it) },
                confirmButton = {
                    Button(onClick = { viewModel.clearError() }) {
                        Text(text = "确定")
                    }
                }
            )
        }
    }
}


