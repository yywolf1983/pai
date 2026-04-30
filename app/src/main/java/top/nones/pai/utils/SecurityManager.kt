package top.nones.pai.utils

import android.content.Context
import android.os.Environment
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

    private val allowedPublicDirectories = setOf(
        "/storage/emulated/0/Documents"
    )

    fun initialize(context: Context) {
        safeDirectories.add(context.filesDir.absolutePath)
        safeDirectories.add(context.cacheDir.absolutePath)
        Environment.getExternalStorageDirectory()?.let {
            safeDirectories.add(it.absolutePath)
        }
    }

    fun isValidPath(path: String): Boolean {
        val absolutePath = File(path).absolutePath

        for (restrictedDir in restrictedDirectories) {
            if (absolutePath.startsWith(restrictedDir)) {
                return false
            }
        }

        for (allowedDir in allowedPublicDirectories) {
            if (absolutePath.startsWith(allowedDir)) {
                return true
            }
        }

        for (safeDir in safeDirectories) {
            if (absolutePath.startsWith(safeDir)) {
                return true
            }
        }

        return false
    }

    fun preventPathTraversal(baseDir: String, targetPath: String): String {
        val resolved = File(baseDir, targetPath).canonicalPath
        if (!resolved.startsWith(baseDir)) {
            throw SecurityException("Illegal path traversal detected")
        }
        return resolved
    }

    fun isSafePath(context: Context, path: String): Boolean {
        val file = FileUtils.getFile(context, path)
        val absolutePath = file.absolutePath

        // 检查是否在受限目录内
        for (restrictedDir in restrictedDirectories) {
            if (absolutePath.startsWith(restrictedDir)) {
                return false
            }
        }

        // 检查是否在安全目录内
        for (safeDir in safeDirectories) {
            if (absolutePath.startsWith(safeDir)) {
                return true
            }
        }

        // 检查是否在允许的公共目录内
        for (allowedDir in allowedPublicDirectories) {
            if (absolutePath.startsWith(allowedDir)) {
                return true
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
        val bindingManager = DirectoryBindingManager(context)
        val boundDir = bindingManager.getBoundDirectory()
        
        if (boundDir != null) {
            val file = FileUtils.getFile(context, operation.filePath, useBoundDir = true)
            val absolutePath = file.absolutePath
            
            for (restrictedDir in restrictedDirectories) {
                if (absolutePath.startsWith(restrictedDir)) {
                    return false
                }
            }
            
            return true
        }
        
        return true
    }
}
