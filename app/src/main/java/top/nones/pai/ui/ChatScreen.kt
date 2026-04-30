package top.nones.pai.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit

import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import top.nones.pai.data.model.Message
import top.nones.pai.data.model.Attachment
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<Message>,
    isSending: Boolean,
    isStreaming: Boolean,
    streamingContent: String,
    thinkingContent: String?,
    outputSpeed: Double,
    contextSize: Int,
    outputSize: Int,
    chatTitle: String,
    editingMessage: Message?,
    selectedAttachments: List<Attachment>,
    onSendMessage: (String) -> Unit,
    onSelectAttachments: () -> Unit,
    onRemoveAttachment: (Int) -> Unit,
    onDeleteMessage: (Message) -> Unit,
    onResendMessage: (Message) -> Unit,
    onEditMessage: (Message) -> Unit,
    onRenameChat: (String) -> Unit,
    onCancelAiRequest: () -> Unit,
    onNewChat: () -> Unit,
    onBack: () -> Unit,
    aiStatus: String?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    var isEditing by remember { mutableStateOf(false) }
                    var editedTitle by remember { mutableStateOf(chatTitle) }
                    
                    if (isEditing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = editedTitle,
                                onValueChange = { editedTitle = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            TextButton(
                                onClick = {
                                    onRenameChat(editedTitle)
                                    isEditing = false
                                }
                            ) {
                                Text(text = "保存")
                            }
                            TextButton(
                                onClick = {
                                    isEditing = false
                                    editedTitle = chatTitle
                                }
                            ) {
                                Text(text = "取消")
                            }
                        }
                    } else {
                        Text(
                            text = chatTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clickable { 
                                    isEditing = true
                                    editedTitle = chatTitle
                                }
                                .padding(end = 8.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNewChat,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "新建聊天"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),

            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                // 消息列表
                val listState = rememberLazyListState()
                
                // 确保初始加载时滚动到最后
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(100)
                    if (listState.layoutInfo.totalItemsCount > 0) {
                        listState.scrollToItem(
                            index = listState.layoutInfo.totalItemsCount - 1,
                            scrollOffset = Int.MAX_VALUE
                        )
                    }
                }
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    state = listState
                ) {
                    items(messages) {
                        MessageItem(message = it, onDelete = onDeleteMessage, onResend = onResendMessage, onEdit = onEditMessage)
                    }
                    
                    // AI 状态显示
                    if (isSending) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                // 思考过程显示
                                if (thinkingContent != null) {
                                    Spacer(modifier = Modifier.size(8.dp))
                                    var isThinkingExpanded by remember { mutableStateOf(true) }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { isThinkingExpanded = !isThinkingExpanded },
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isThinkingExpanded) "▼ 思考中" else "▶ 思考中",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${thinkingContent.length} 字符",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                            if (isThinkingExpanded) {
                                                Spacer(modifier = Modifier.size(4.dp))
                                                Text(
                                                    text = thinkingContent,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // 流式输出显示
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Text(
                                            text = "AI 回复:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                        // 使用优化后的MarkdownText渲染，添加内边距防止表格渲染时抖动
                                        MarkdownText(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            markdown = streamingContent ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            syntaxHighlightColor = MaterialTheme.colorScheme.surface,
                                            syntaxHighlightTextColor = MaterialTheme.colorScheme.onSurface,
                                            wrapMultilineTextWidth = true
                                        )
                                    }
                                }
                                

                                

                            }
                        }
                    }
                }

                // 模型数据信息
                if (isSending) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "上下文: ${contextSize} 字符",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "输出: ${outputSize} 字符",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "速度: ${String.format("%.1f", outputSpeed)} t/s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 选中的附件显示
                if (selectedAttachments.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "已选择附件:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            selectedAttachments.forEachIndexed { index, attachment ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "附件",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = attachment.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = attachment.type,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { 
                                            // 移除附件
                                            onRemoveAttachment(index)
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "移除附件",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 输入区域
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var messageText by remember {
                            mutableStateOf("")
                        }

                        // 当editingMessage变化时，更新输入框内容
                        LaunchedEffect(editingMessage) {
                            if (editingMessage != null) {
                                messageText = editingMessage.content
                            }
                        }

                        IconButton(
                            onClick = onSelectAttachments,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "选择附件",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .weight(1f)
                                .animateContentSize(),
                            placeholder = {
                                Text(
                                    text = "输入消息...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            minLines = 1,
                            maxLines = 5,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        val buttonColor by animateColorAsState(
                            targetValue = if (isSending || (messageText.trim().isEmpty() && selectedAttachments.isEmpty())) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            animationSpec = tween(durationMillis = 200)
                        )

                        Button(
                            onClick = {
                                if (isSending) {
                                    onCancelAiRequest()
                                } else {
                                    val trimmed = messageText.trim()
                                    if (trimmed.isNotEmpty() || selectedAttachments.isNotEmpty()) {
                                        onSendMessage(trimmed)
                                        messageText = ""
                                    }
                                }
                            },
                            enabled = !isSending || (messageText.trim().isNotEmpty() || selectedAttachments.isNotEmpty()) || isSending,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSending) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    buttonColor
                                },
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = Color.White,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .size(40.dp)
                                .padding(2.dp)
                                .animateContentSize()
                        ) {
                            if (isSending) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "取消",
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "发送",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun MessageItem(message: Message, onDelete: (Message) -> Unit, onResend: (Message) -> Unit, onEdit: (Message) -> Unit) {
    val isUser = message.role == "user"
    val isFailed = message.content.startsWith("错误:")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // 消息标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isUser) {
                Text(
                    text = "用户",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { onEdit(message) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "编辑消息",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { onDelete(message) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除消息",
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (isFailed) {
                    IconButton(
                        onClick = { onResend(message) },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "重新发送",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { onDelete(message) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除消息",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth(1f)
                .animateContentSize(),
            shape = if (isUser) {
                RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
            } else {
                RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
            },
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),

        ) {
            Row(
                modifier = Modifier
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    if (isUser) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = message.content ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isFailed) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 0.9
                            )
                            // 显示附件
                            if (message.attachments.isNotEmpty()) {
                                Spacer(modifier = Modifier.size(8.dp))
                                Column {
                                    Text(
                                        text = "附件:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isFailed) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                                    )
                                    message.attachments.forEach { attachment ->
                                        Row(
                                            modifier = Modifier.padding(top = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "附件",
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isFailed) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${attachment.name} (${attachment.type})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isFailed) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 对AI回复的消息使用Markdown组件
                        MarkdownText(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            markdown = message.content ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            syntaxHighlightColor = MaterialTheme.colorScheme.surface,
                            syntaxHighlightTextColor = MaterialTheme.colorScheme.onSurface,
                            wrapMultilineTextWidth = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedEllipsis() {
    val animationValue by animateIntAsState(
        targetValue = 3,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "animationValue"
    )
    
    Row {
        repeat(3) {index ->
            Text(
                text = if (index < animationValue) "." else " ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


