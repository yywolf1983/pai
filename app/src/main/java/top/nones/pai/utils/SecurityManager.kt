package top.nones.pai.utils

import android.content.Context
import java.io.File

object SecurityManager {
    private val safeDirectories = mutableSetOf<String>()
    private val restrictedDirectories = setOf(
        "/system",
        "/data",
        "/dev",
        "/proc",
        "/sys"
    )

    fun initialize(context: Context) {
        // 初始化安全目录，只允许访问应用内部存储和指定的安全目录
        safeDirectories.add(context.filesDir.absolutePath)
        safeDirectories.add(context.cacheDir.absolutePath)
        // 可以添加其他安全目录
    }

    fun isSafePath(context: Context, path: String): Boolean {
        val file = FileUtils.getFile(context, path)
        val absolutePath = file.absolutePath

        // 检查是否在安全目录内
        for (safeDir in safeDirectories) {
            if (absolutePath.startsWith(safeDir)) {
                return true
            }
        }

        // 检查是否在受限目录内
        for (restrictedDir in restrictedDirectories) {
            if (absolutePath.startsWith(restrictedDir)) {
                return false
            }
        }

        // 默认为不安全
        return false
    }

    fun addSafeDirectory(directory: String) {
        safeDirectories.add(directory)
    }

    fun removeSafeDirectory(directory: String) {
        safeDirectories.remove(directory)
    }

    fun getSafeDirectories(): Set<String> {
        return safeDirectories
    }

    fun checkOperationSafety(context: Context, operation: FileOperationParser.FileOperation): Boolean {
        // 检查路径安全性
        if (!isSafePath(context, operation.filePath)) {
            return false
        }

        // 对于移动和复制操作，还需要检查目标路径
        if (operation.type in setOf(
                FileOperationParser.OperationType.MOVE,
                FileOperationParser.OperationType.COPY,
                FileOperationParser.OperationType.BATCH_MOVE
            )) {
            if (!isSafePath(context, operation.content)) {
                return false
            }
        }

        // 对于批量操作，检查目录安全性
        if (operation.type in setOf(
                FileOperationParser.OperationType.BATCH_DELETE,
                FileOperationParser.OperationType.BATCH_RENAME,
                FileOperationParser.OperationType.SMART_SORT
            )) {
            if (!isSafePath(context, operation.filePath)) {
                return false
            }
        }

        return true
    }
}
