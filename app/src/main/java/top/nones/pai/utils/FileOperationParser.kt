package top.nones.pai.utils

class FileOperationParser {
    data class FileOperation(
        val type: OperationType,
        val filePath: String,
        val content: String = "",
        val options: Map<String, String> = emptyMap()
    )

    enum class OperationType {
        READ,
        WRITE,
        CREATE,
        DELETE,
        LIST,
        EXIST,
        SIZE,
        RENAME,
        MOVE,
        COPY,
        BATCH_RENAME,
        BATCH_MOVE,
        BATCH_DELETE,
        SMART_SORT,
        BIND_DIRECTORY,
        UNBIND_DIRECTORY,
        SHOW_BOUND_DIRECTORY,
        APPEND,
        UNKNOWN
    }

    fun parseCommand(command: String): FileOperation {
        try {
            val json = org.json.JSONObject(command.trim())
            val name = json.optString("name")
            val params = json.optJSONObject("parameters")
            
            when (name) {
                "write_file" -> {
                    val filePath = params?.optString("file_path", "") ?: ""
                    val content = params?.optString("content", "") ?: ""
                    return FileOperation(OperationType.WRITE, filePath, content)
                }
                "read_file" -> {
                    val filePath = params?.optString("file_path", "") ?: ""
                    return FileOperation(OperationType.READ, filePath)
                }
                "delete_file" -> {
                    val filePath = params?.optString("file_path", "") ?: ""
                    return FileOperation(OperationType.DELETE, filePath)
                }
                "create_file" -> {
                    val filePath = params?.optString("file_path", "") ?: ""
                    val content = params?.optString("content", "") ?: ""
                    return FileOperation(OperationType.CREATE, filePath, content)
                }
                "append_to_file" -> {
                    val filePath = params?.optString("file_path", "") ?: ""
                    val content = params?.optString("content", "") ?: ""
                    return FileOperation(OperationType.APPEND, filePath, content)
                }
                "list_directory" -> {
                    val directory = params?.optString("directory", "") ?: ""
                    return FileOperation(OperationType.LIST, directory)
                }
                "bind_directory" -> {
                    val directory = params?.optString("directory", "") ?: ""
                    return FileOperation(OperationType.BIND_DIRECTORY, directory)
                }
                "unbind_directory" -> {
                    return FileOperation(OperationType.UNBIND_DIRECTORY, "")
                }
                "show_bound_directory" -> {
                    return FileOperation(OperationType.SHOW_BOUND_DIRECTORY, "")
                }
                "check_file_exists" -> {
                    val filePath = params?.optString("file_path", "") ?: ""
                    return FileOperation(OperationType.EXIST, filePath)
                }
                "get_file_size" -> {
                    val filePath = params?.optString("file_path", "") ?: ""
                    return FileOperation(OperationType.SIZE, filePath)
                }
                "rename_file" -> {
                    val filePath = params?.optString("file_path", "") ?: ""
                    val newName = params?.optString("new_name", "") ?: ""
                    return FileOperation(OperationType.RENAME, filePath, newName)
                }
                "move_file" -> {
                    val sourcePath = params?.optString("source_path", "") ?: ""
                    val targetPath = params?.optString("target_path", "") ?: ""
                    return FileOperation(OperationType.MOVE, sourcePath, targetPath)
                }
                "copy_file" -> {
                    val sourcePath = params?.optString("source_path", "") ?: ""
                    val targetPath = params?.optString("target_path", "") ?: ""
                    return FileOperation(OperationType.COPY, sourcePath, targetPath)
                }
                "batch_delete_files" -> {
                    val directory = params?.optString("directory", "") ?: ""
                    return FileOperation(OperationType.BATCH_DELETE, directory)
                }
                "smart_sort_files" -> {
                    val directory = params?.optString("directory", "") ?: ""
                    val sortBy = params?.optString("sort_by", "type") ?: "type"
                    return FileOperation(OperationType.SMART_SORT, directory, options = mapOf("sort_by" to sortBy))
                }
            }
        } catch (e: Exception) {
        }

        val bracketWritePattern = "\\[写入文件:\\s*([^\\]]+)\\](.*)".toRegex()
        bracketWritePattern.find(command)?.let {
            val filePath = it.groupValues[1].trim()
            val content = it.groupValues[2].trim()
            return FileOperation(OperationType.WRITE, filePath, content)
        }

        val bracketReadPattern = "\\[读取文件:\\s*([^\\]]+)\\]".toRegex()
        bracketReadPattern.find(command)?.let {
            return FileOperation(OperationType.READ, it.groupValues[1].trim())
        }

        val bracketDeletePattern = "\\[删除文件:\\s*([^\\]]+)\\]".toRegex()
        bracketDeletePattern.find(command)?.let {
            return FileOperation(OperationType.DELETE, it.groupValues[1].trim())
        }

        val bracketCreatePattern = "\\[创建文件:\\s*([^\\]]+)\\](.*)".toRegex()
        bracketCreatePattern.find(command)?.let {
            val filePath = it.groupValues[1].trim()
            val content = it.groupValues[2].trim()
            return FileOperation(OperationType.CREATE, filePath, content)
        }

        val bracketAppendPattern = "\\[追加内容:\\s*([^\\]]+)\\](.*)".toRegex()
        bracketAppendPattern.find(command)?.let {
            val filePath = it.groupValues[1].trim()
            val content = it.groupValues[2].trim()
            return FileOperation(OperationType.APPEND, filePath, content)
        }

        val bracketListPattern = "\\[列出目录\\]".toRegex()
        if (bracketListPattern.containsMatchIn(command)) {
            return FileOperation(OperationType.LIST, "")
        }

        val bracketDirPattern = "\\[绑定目录:\\s*([^\\]]+)\\]".toRegex()
        bracketDirPattern.find(command)?.let {
            return FileOperation(OperationType.BIND_DIRECTORY, it.groupValues[1].trim())
        }

        val bracketShowDirPattern = "\\[当前目录\\]".toRegex()
        if (bracketShowDirPattern.containsMatchIn(command)) {
            return FileOperation(OperationType.SHOW_BOUND_DIRECTORY, "")
        }

        val lowerCommand = command.lowercase()
        when {
            lowerCommand.contains("绑定") && lowerCommand.contains("目录") -> {
                val path = extractFilePath(lowerCommand)
                return FileOperation(OperationType.BIND_DIRECTORY, path)
            }
            lowerCommand.contains("取消绑定") || lowerCommand.contains("清除绑定") || lowerCommand.contains("解绑") -> {
                return FileOperation(OperationType.UNBIND_DIRECTORY, "")
            }
            lowerCommand.contains("当前目录") || (lowerCommand.contains("绑定目录") && !lowerCommand.contains("绑定到")) -> {
                return FileOperation(OperationType.SHOW_BOUND_DIRECTORY, "")
            }
            (lowerCommand.contains("添加") && lowerCommand.contains("到")) || lowerCommand.contains("追加") -> {
                val (filePath, content) = extractFilePathAndContent(lowerCommand)
                return FileOperation(OperationType.APPEND, filePath, content)
            }
            lowerCommand.contains("读取") || lowerCommand.contains("查看") || lowerCommand.contains("打开") -> {
                val filePath = extractFilePath(lowerCommand)
                return FileOperation(OperationType.READ, filePath)
            }
            lowerCommand.contains("写入") || lowerCommand.contains("保存") || lowerCommand.contains("修改") -> {
                val (filePath, content) = extractFilePathAndContent(lowerCommand)
                return FileOperation(OperationType.WRITE, filePath, content)
            }
            lowerCommand.contains("创建") || lowerCommand.contains("新建") -> {
                val filePath = extractFilePath(lowerCommand)
                return FileOperation(OperationType.CREATE, filePath)
            }
            lowerCommand.contains("删除") || lowerCommand.contains("移除") -> {
                if (lowerCommand.contains("批量") || lowerCommand.contains("所有")) {
                    val filePath = extractFilePath(lowerCommand)
                    return FileOperation(OperationType.BATCH_DELETE, filePath)
                } else {
                    val filePath = extractFilePath(lowerCommand)
                    return FileOperation(OperationType.DELETE, filePath)
                }
            }
            lowerCommand.contains("列出") || lowerCommand.contains("查看") && lowerCommand.contains("目录") -> {
                val directoryPath = extractFilePath(lowerCommand)
                return FileOperation(OperationType.LIST, directoryPath)
            }
            lowerCommand.contains("是否存在") || lowerCommand.contains("存在") -> {
                val filePath = extractFilePath(lowerCommand)
                return FileOperation(OperationType.EXIST, filePath)
            }
            lowerCommand.contains("大小") || lowerCommand.contains("尺寸") -> {
                val filePath = extractFilePath(lowerCommand)
                return FileOperation(OperationType.SIZE, filePath)
            }
            lowerCommand.contains("重命名") -> {
                if (lowerCommand.contains("批量") || lowerCommand.contains("所有")) {
                    val filePath = extractFilePath(lowerCommand)
                    val options = extractOptions(lowerCommand)
                    return FileOperation(OperationType.BATCH_RENAME, filePath, options = options)
                } else {
                    val (filePath, newName) = extractRenameInfo(lowerCommand)
                    return FileOperation(OperationType.RENAME, filePath, newName)
                }
            }
            lowerCommand.contains("移动") -> {
                if (lowerCommand.contains("批量") || lowerCommand.contains("所有")) {
                    val (sourcePath, targetPath) = extractMoveCopyInfo(lowerCommand)
                    return FileOperation(OperationType.BATCH_MOVE, sourcePath, targetPath)
                } else {
                    val (sourcePath, targetPath) = extractMoveCopyInfo(lowerCommand)
                    return FileOperation(OperationType.MOVE, sourcePath, targetPath)
                }
            }
            lowerCommand.contains("复制") -> {
                val (sourcePath, targetPath) = extractMoveCopyInfo(lowerCommand)
                return FileOperation(OperationType.COPY, sourcePath, targetPath)
            }
            lowerCommand.contains("整理") || lowerCommand.contains("分类") -> {
                val directoryPath = extractFilePath(lowerCommand)
                val options = extractOptions(lowerCommand)
                return FileOperation(OperationType.SMART_SORT, directoryPath, options = options)
            }
            else -> {
                return parseNaturalLanguageDescription(command)
            }
        }
    }

    private fun parseNaturalLanguageDescription(command: String): FileOperation {
        val lowerCommand = command.lowercase()
        
        val commonExtensions = "txt|py|md|json|java|cpp|xml|csv|log|sh|bat|js|html|css|php|go|rs|swift|kt|dart"
        
        val writePattern = "(写入|保存|生成).*文件?\\s*([\\w\\u4e00-\\u9fa5]+\\.($commonExtensions))".toRegex(RegexOption.IGNORE_CASE)
        writePattern.find(lowerCommand)?.let { match ->
            val fileName = match.groupValues[2]
            return FileOperation(OperationType.WRITE, fileName)
        }
        
        val readPattern = "(读取|查看|打开|显示).*文件?\\s*([\\w\\u4e00-\\u9fa5]+\\.($commonExtensions))".toRegex(RegexOption.IGNORE_CASE)
        readPattern.find(lowerCommand)?.let { match ->
            return FileOperation(OperationType.READ, match.groupValues[2])
        }
        
        val deletePattern = "(删除|移除|删除文件).*文件?\\s*([\\w\\u4e00-\\u9fa5]+\\.($commonExtensions))".toRegex(RegexOption.IGNORE_CASE)
        deletePattern.find(lowerCommand)?.let { match ->
            return FileOperation(OperationType.DELETE, match.groupValues[2])
        }
        
        val createPattern = "(创建|新建|创建文件).*文件?\\s*([\\w\\u4e00-\\u9fa5]+\\.($commonExtensions))".toRegex(RegexOption.IGNORE_CASE)
        createPattern.find(lowerCommand)?.let { match ->
            return FileOperation(OperationType.CREATE, match.groupValues[2])
        }
        
        val pathPattern = "/[^\\s]+\\.($commonExtensions)".toRegex(RegexOption.IGNORE_CASE)
        pathPattern.find(command)?.let { match ->
            val fullPath = match.value
            val fileName = fullPath.substringAfterLast("/")
            if (lowerCommand.contains("写入") || lowerCommand.contains("保存") || lowerCommand.contains("生成")) {
                return FileOperation(OperationType.WRITE, fileName)
            } else if (lowerCommand.contains("读取") || lowerCommand.contains("查看") || lowerCommand.contains("打开")) {
                return FileOperation(OperationType.READ, fileName)
            } else if (lowerCommand.contains("删除") || lowerCommand.contains("移除")) {
                return FileOperation(OperationType.DELETE, fileName)
            }
        }
        
        val fileNameOnlyPattern = "\\b([\\w\\u4e00-\\u9fa5]+\\.($commonExtensions))\\b".toRegex(RegexOption.IGNORE_CASE)
        fileNameOnlyPattern.find(command)?.let { match ->
            val fileName = match.groupValues[1]
            if (lowerCommand.contains("写入") || lowerCommand.contains("保存") || lowerCommand.contains("生成")) {
                return FileOperation(OperationType.WRITE, fileName)
            } else if (lowerCommand.contains("读取") || lowerCommand.contains("查看") || lowerCommand.contains("打开")) {
                return FileOperation(OperationType.READ, fileName)
            } else if (lowerCommand.contains("删除") || lowerCommand.contains("移除")) {
                return FileOperation(OperationType.DELETE, fileName)
            }
        }
        
        return FileOperation(OperationType.UNKNOWN, "")
    }

    fun getToolDescription(): String {
        return """
你必须使用以下JSON格式的工具调用来操作文件：

【文件操作】
写入文件：{"name":"write_file","parameters":{"file_path":"文件名","content":"内容"}}
读取文件：{"name":"read_file","parameters":{"file_path":"文件名"}}
删除文件：{"name":"delete_file","parameters":{"file_path":"文件名"}}
创建文件：{"name":"create_file","parameters":{"file_path":"文件名","content":"内容"}}
追加内容：{"name":"append_to_file","parameters":{"file_path":"文件名","content":"追加内容"}}
检查存在：{"name":"check_file_exists","parameters":{"file_path":"文件名"}}
获取大小：{"name":"get_file_size","parameters":{"file_path":"文件名"}}
重命名：{"name":"rename_file","parameters":{"file_path":"原文件名","new_name":"新文件名"}}
移动文件：{"name":"move_file","parameters":{"source_path":"源文件","target_path":"目标文件"}}
复制文件：{"name":"copy_file","parameters":{"source_path":"源文件","target_path":"目标文件"}}

【目录操作】
列出目录：{"name":"list_directory","parameters":{"directory":"目录路径"}}
绑定目录：{"name":"bind_directory","parameters":{"directory":"目录路径"}}
取消绑定：{"name":"unbind_directory","parameters":{}}
查看目录：{"name":"show_bound_directory","parameters":{}}
批量删除：{"name":"batch_delete_files","parameters":{"directory":"目录路径"}}
智能整理：{"name":"smart_sort_files","parameters":{"directory":"目录路径","sort_by":"type"}}

规则：
1. 必须使用JSON格式，不要用自然语言描述操作
2. 只需提供文件名，不要使用绝对路径（如 /data/xxx）
3. 所有操作会自动在绑定目录下进行
4. 如果需要写入内容，请在content参数中提供完整内容
""".trim()
    }

    private fun extractFilePath(command: String): String {
        val extensionPattern = "\\b([\\w\\u4e00-\\u9fa5]+\\.[a-zA-Z]{2,4})\\b".toRegex()
        val matches = extensionPattern.findAll(command)
        for (match in matches) {
            return match.groupValues[1]
        }
        
        val pathPattern = "[a-zA-Z0-9_./\\-\\u4e00-\\u9fa5]+".toRegex()
        val pathMatches = pathPattern.findAll(command).toList().reversed()
        for (match in pathMatches) {
            val potentialPath = match.value
            if (!potentialPath.startsWith("/sdcard/") && !potentialPath.startsWith("/storage/")) {
                return potentialPath
            }
        }
        
        return ""
    }

    private fun extractFilePathAndContent(command: String): Pair<String, String> {
        val filePath = extractFilePath(command)
        if (filePath.isEmpty()) {
            return Pair("", "")
        }
        
        val filePathIndex = command.indexOf(filePath)
        
        val writeKeywords = listOf("写入", "保存", "修改", "写")
        for (keyword in writeKeywords) {
            val keywordIndex = command.indexOf(keyword)
            if (keywordIndex >= 0 && keywordIndex < filePathIndex) {
                val afterFilePath = command.substring(filePathIndex + filePath.length).trim()
                var content = afterFilePath.replaceFirst("^[:,：]\\s*", "")
                content = content.replaceFirst("^是\\s*", "")
                content = content.replaceFirst("^内容\\s*", "")
                content = content.replaceFirst("^为\\s*", "")
                return Pair(filePath, content)
            }
        }
        
        val beforeFilePath = command.substring(0, filePathIndex).trim()
        if (beforeFilePath.contains("到") || beforeFilePath.contains("入")) {
            val afterFilePath = command.substring(filePathIndex + filePath.length).trim()
            var content = afterFilePath.replaceFirst("^[:,：]\\s*", "")
            content = content.replaceFirst("^是\\s*", "")
            return Pair(filePath, content)
        }
        
        for (keyword in writeKeywords) {
            val keywordIndex = command.indexOf(keyword)
            if (keywordIndex > filePathIndex) {
                val beforeKeyword = command.substring(filePathIndex + filePath.length, keywordIndex).trim()
                return Pair(filePath, beforeKeyword)
            }
        }
        
        val afterFilePath = command.substring(filePathIndex + filePath.length).trim()
        var content = afterFilePath.replaceFirst("^[:,：]\\s*", "")
        return Pair(filePath, content)
    }

    private fun extractRenameInfo(command: String): Pair<String, String> {
        val filePath = extractFilePath(command)
        // 提取新文件名
        val renameIndex = command.indexOf("重命名")
        val contentStartIndex = command.indexOf("为", renameIndex)
        if (contentStartIndex > 0) {
            val newName = command.substring(contentStartIndex + 1).trim()
            return Pair(filePath, newName)
        }
        return Pair(filePath, "")
    }

    private fun extractMoveCopyInfo(command: String): Pair<String, String> {
        val sourcePath = extractFilePath(command)
        // 提取目标路径
        val moveIndex = command.indexOf("移动")
        val copyIndex = command.indexOf("复制")
        val actionIndex = if (moveIndex > 0) moveIndex else copyIndex
        val toIndex = command.indexOf("到", actionIndex)
        if (toIndex > 0) {
            val targetPath = command.substring(toIndex + 1).trim()
            return Pair(sourcePath, targetPath)
        }
        return Pair(sourcePath, "")
    }

    private fun extractOptions(command: String): Map<String, String> {
        val options = mutableMapOf<String, String>()
        
        // 提取分类选项
        if (command.contains("按类型")) {
            options["sort_by"] = "type"
        } else if (command.contains("按日期")) {
            options["sort_by"] = "date"
        } else if (command.contains("按大小")) {
            options["sort_by"] = "size"
        }
        
        // 提取目标目录
        val toIndex = command.indexOf("到")
        if (toIndex > 0) {
            val targetPath = command.substring(toIndex + 1).trim()
            options["target"] = targetPath
        }
        
        return options
    }
}
