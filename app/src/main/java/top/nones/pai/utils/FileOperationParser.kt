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
        UNKNOWN
    }

    fun parseCommand(command: String): FileOperation {
        val lowerCommand = command.lowercase()

        when {
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
                return FileOperation(OperationType.UNKNOWN, "")
            }
        }
    }

    private fun extractFilePath(command: String): String {
        // 简单的路径提取，实际应用中可能需要更复杂的解析
        val pathPattern = "[a-zA-Z0-9_./\\-]+".toRegex()
        val matches = pathPattern.findAll(command)
        for (match in matches) {
            val potentialPath = match.value
            // 检查是否可能是文件路径
            if (potentialPath.contains(".") || potentialPath.contains("/") || potentialPath.contains("\\") || potentialPath == "." || potentialPath == "..") {
                return potentialPath
            }
        }
        return ""
    }

    private fun extractFilePathAndContent(command: String): Pair<String, String> {
        val filePath = extractFilePath(command)
        // 提取内容（简单实现，实际应用中可能需要更复杂的解析）
        val contentStartIndex = command.indexOf(filePath) + filePath.length
        val content = command.substring(contentStartIndex).trim()
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
