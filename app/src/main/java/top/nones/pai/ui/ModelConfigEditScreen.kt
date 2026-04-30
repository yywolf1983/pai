package top.nones.pai.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.widget.Toast
import android.content.Context
import top.nones.pai.data.model.ModelConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigEditScreen(
    context: Context,
    modelConfig: ModelConfig? = null,
    isTesting: Boolean,
    isFetchingModels: Boolean,
    testStatus: String?,
    availableModels: List<String>,
    onTest: (ModelConfig) -> Unit,
    onFetchModels: (ModelConfig) -> Unit,
    onClearAvailableModels: () -> Unit,
    onClearTestStatus: () -> Unit,
    onSave: (ModelConfig) -> Unit,
    onCancel: () -> Unit
) {
    // 名称
    var name by remember {
        mutableStateOf(modelConfig?.name ?: "")
    }
    // API URL
    var endpoint by remember {
        mutableStateOf(modelConfig?.endpoint ?: "")
    }
    // API Key
    var apiKey by remember {
        mutableStateOf(modelConfig?.apiKey ?: "")
    }
    // 模型名称
    var modelName by remember {
        mutableStateOf(modelConfig?.modelName ?: "")
    }
    var systemPrompt by remember {
        mutableStateOf(modelConfig?.systemPrompt ?: "")
    }
    var isLocal by remember {
        mutableStateOf(modelConfig?.isLocal ?: false)
    }
    // 默认模型
    var isDefault by remember {
        mutableStateOf(modelConfig?.isDefault ?: false)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(text = if (modelConfig == null) "新建模型配置" else "编辑模型配置") }
        )

        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = "名称") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text(text = "Endpoint (例如 http://10.0.2.2:11434)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(text = "API Key") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text(text = "模型名称") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text(text = "System Prompt（可选）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isLocal,
                            onCheckedChange = { isLocal = it }
                        )
                        Text(text = "本地模型")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isDefault,
                            onCheckedChange = { isDefault = it }
                        )
                        Text(text = "设为默认模型")
                    }
                }

                Button(
                    onClick = {
                        val candidate = buildCandidateModelConfig(
                            modelConfig = modelConfig,
                            name = name,
                            endpoint = endpoint,
                            apiKey = apiKey,
                            modelName = modelName,
                            systemPrompt = systemPrompt,
                            isLocal = isLocal,
                            isDefault = isDefault
                        ) ?: return@Button
                        onTest(candidate)
                    },
                    enabled = !isTesting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isTesting) "测试中..." else "测试连接")
                }

                Button(
                    onClick = {
                        val candidate = buildCandidateModelConfig(
                            modelConfig = modelConfig,
                            name = name,
                            endpoint = endpoint,
                            apiKey = apiKey,
                            modelName = modelName,
                            systemPrompt = systemPrompt,
                            isLocal = isLocal,
                            isDefault = isDefault
                        ) ?: return@Button
                        onFetchModels(candidate)
                    },
                    enabled = !isFetchingModels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(text = if (isFetchingModels) "获取中..." else "自动获取模型")
                }

                if (availableModels.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        items(availableModels) { item ->
                            AssistChip(
                                onClick = {
                                    modelName = item
                                },
                                label = { Text(item) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                    TextButton(onClick = onClearAvailableModels) {
                        Text(text = "清除模型列表")
                    }
                }
                testStatus?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.startsWith("连接成功")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    TextButton(onClick = onClearTestStatus) {
                        Text(text = "清除测试结果")
                    }
                }
            }
        }

        // 按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = "取消")
            }
            Button(
                onClick = {
                    val candidate = buildCandidateModelConfig(
                        modelConfig = modelConfig,
                        name = name,
                        endpoint = endpoint,
                        apiKey = apiKey,
                        modelName = modelName,
                        systemPrompt = systemPrompt,
                        isLocal = isLocal,
                        isDefault = isDefault
                    )
                    if (candidate == null) {
                        Toast.makeText(context, "请填写必填字段：名称、Endpoint和模型名称", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSave(candidate)
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = "保存")
            }
        }
    }
}

private fun buildCandidateModelConfig(
    modelConfig: ModelConfig?,
    name: String,
    endpoint: String,
    apiKey: String,
    modelName: String,
    systemPrompt: String,
    isLocal: Boolean,
    isDefault: Boolean
): ModelConfig? {
    val safeName = name.trim()
    val safeEndpoint = endpoint.trim()
    val safeApiKey = apiKey.trim()
    val safeModelName = modelName.trim()
    if (safeName.isEmpty() || safeEndpoint.isEmpty() || safeModelName.isEmpty()) {
        return null
    }
    return ModelConfig(
        id = modelConfig?.id ?: 0,
        name = safeName,
        endpoint = safeEndpoint,
        apiKey = safeApiKey,
        modelName = safeModelName,
        systemPrompt = systemPrompt.trim(),
        isLocal = isLocal,
        isDefault = isDefault
    )
}
