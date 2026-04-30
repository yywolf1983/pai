package top.nones.pai.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Base64
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

object FileUtils {
    fun readFile(context: Context, filePath: String): String {
        val file = getFile(context, filePath)
        if (!file.exists()) {
            throw IOException("文件不存在: $filePath")
        }
        return file.readText()
    }

    fun writeFile(context: Context, filePath: String, content: String): Boolean {
        val file = getFile(context, filePath)
        try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            return true
        } catch (e: SecurityException) {
            return tryWriteToAppDir(context, filePath, content)
        } catch (e: Exception) {
            println("File write failed: ${e.message}, path: ${file.absolutePath}")
            return false
        }
    }

    fun appendToFile(context: Context, filePath: String, content: String): Boolean {
        val file = getFile(context, filePath)
        try {
            file.parentFile?.mkdirs()
            file.appendText(content)
            return true
        } catch (e: SecurityException) {
            return tryAppendToAppDir(context, filePath, content)
        } catch (e: Exception) {
            return false
        }
    }

    private fun tryWriteToAppDir(context: Context, filePath: String, content: String): Boolean {
        val fileName = File(filePath).name
        val appFile = File(context.filesDir, fileName)
        try {
            appFile.writeText(content)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun tryAppendToAppDir(context: Context, filePath: String, content: String): Boolean {
        val fileName = File(filePath).name
        val appFile = File(context.filesDir, fileName)
        try {
            appFile.appendText(content)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun createFile(context: Context, filePath: String): Boolean {
        val file = getFile(context, filePath)
        try {
            // 确保父目录存在
            file.parentFile?.mkdirs()
            return file.createNewFile()
        } catch (e: Exception) {
            return false
        }
    }

    fun deleteFile(context: Context, filePath: String): Boolean {
        val file = getFile(context, filePath)
        return file.delete()
    }

    fun listFiles(context: Context, directoryPath: String): List<String> {
        val pathToUse = if (directoryPath.isEmpty()) {
            val bindingManager = DirectoryBindingManager(context)
            bindingManager.getBoundDirectory() ?: "."
        } else {
            directoryPath
        }
        val directory = getFile(context, pathToUse)
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }
        return directory.listFiles()?.map { it.name } ?: emptyList()
    }

    fun fileExists(context: Context, filePath: String): Boolean {
        val file = getFile(context, filePath)
        return file.exists()
    }

    fun getFileSize(context: Context, filePath: String): Long {
        val file = getFile(context, filePath)
        if (!file.exists()) {
            return 0
        }
        return file.length()
    }

    fun renameFile(context: Context, oldPath: String, newName: String): Boolean {
        val oldFile = getFile(context, oldPath)
        if (!oldFile.exists()) {
            return false
        }
        val newFile = File(oldFile.parentFile, newName)
        return oldFile.renameTo(newFile)
    }

    fun moveFile(context: Context, sourcePath: String, targetPath: String): Boolean {
        val sourceFile = getFile(context, sourcePath)
        val targetFile = getFile(context, targetPath)
        if (!sourceFile.exists()) {
            return false
        }
        try {
            // 确保目标目录存在
            targetFile.parentFile?.mkdirs()
            return sourceFile.renameTo(targetFile)
        } catch (e: Exception) {
            return false
        }
    }

    fun copyFile(context: Context, sourcePath: String, targetPath: String): Boolean {
        val sourceFile = getFile(context, sourcePath)
        val targetFile = getFile(context, targetPath)
        if (!sourceFile.exists()) {
            return false
        }
        try {
            // 确保目标目录存在
            targetFile.parentFile?.mkdirs()
            sourceFile.inputStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun batchDeleteFiles(context: Context, directoryPath: String, pattern: String = "*"): Int {
        val directory = getFile(context, directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            return 0
        }
        var count = 0
        directory.listFiles()?.forEach { file ->
            if (file.name.matches(pattern.toRegex())) {
                if (file.delete()) {
                    count++
                }
            }
        }
        return count
    }

    fun batchRenameFiles(context: Context, directoryPath: String, prefix: String = ""): Int {
        val directory = getFile(context, directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            return 0
        }
        var count = 0
        directory.listFiles()?.forEachIndexed { index, file ->
            val extension = file.extension
            val newName = if (prefix.isNotEmpty()) {
                "${prefix}_${index + 1}${if (extension.isNotEmpty()) ".$extension" else ""}"
            } else {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                "${timestamp}_${index + 1}${if (extension.isNotEmpty()) ".$extension" else ""}"
            }
            val newFile = File(directory, newName)
            if (file.renameTo(newFile)) {
                count++
            }
        }
        return count
    }

    fun batchMoveFiles(context: Context, sourcePath: String, targetPath: String, pattern: String = "*"): Int {
        val sourceDir = getFile(context, sourcePath)
        val targetDir = getFile(context, targetPath)
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            return 0
        }
        try {
            targetDir.mkdirs()
            var count = 0
            sourceDir.listFiles()?.forEach { file ->
                if (file.name.matches(pattern.toRegex())) {
                    val targetFile = File(targetDir, file.name)
                    if (file.renameTo(targetFile)) {
                        count++
                    }
                }
            }
            return count
        } catch (e: Exception) {
            return 0
        }
    }

    fun smartSortFiles(context: Context, directoryPath: String, sortBy: String = "type", targetBasePath: String? = null): Int {
        val sourceDir = getFile(context, directoryPath)
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            return 0
        }
        val targetDir = if (targetBasePath != null) {
            getFile(context, targetBasePath)
        } else {
            sourceDir
        }
        try {
            targetDir.mkdirs()
            var count = 0
            sourceDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val targetSubDir = when (sortBy) {
                        "type" -> {
                            val extension = file.extension.lowercase()
                            when {
                                extension in setOf("txt", "doc", "docx", "pdf", "md") -> "documents"
                                extension in setOf("jpg", "jpeg", "png", "gif", "bmp") -> "images"
                                extension in setOf("mp4", "avi", "mov", "mkv") -> "videos"
                                extension in setOf("mp3", "wav", "flac", "ogg") -> "audio"
                                extension in setOf("zip", "rar", "7z", "tar") -> "archives"
                                extension in setOf("py", "java", "kt", "js", "html", "css") -> "code"
                                else -> "others"
                            }
                        }
                        "date" -> {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
                            dateFormat.format(Date(file.lastModified()))
                        }
                        "size" -> {
                            val size = file.length()
                            when {
                                size < 1024 * 1024 -> "small" // < 1MB
                                size < 1024 * 1024 * 10 -> "medium" // < 10MB
                                else -> "large" // >= 10MB
                            }
                        }
                        else -> "others"
                    }
                    val subDir = File(targetDir, targetSubDir)
                    subDir.mkdirs()
                    val targetFile = File(subDir, file.name)
                    if (file.renameTo(targetFile)) {
                        count++
                    }
                }
            }
            return count
        } catch (e: Exception) {
            return 0
        }
    }

    fun getFile(context: Context, filePath: String, useBoundDir: Boolean = true): File {
        val bindingManager = DirectoryBindingManager(context)
        val boundDir = bindingManager.getBoundDirectory()
        
        if (useBoundDir && boundDir != null) {
            // 提取文件名（去掉路径部分）
            val fileName = if (filePath.startsWith("/")) {
                File(filePath).name
            } else {
                filePath
            }
            return File(boundDir, fileName)
        }

        if (filePath.startsWith("/")) {
            return File(filePath)
        }
        return File("/storage/emulated/0/Documents", filePath)
    }

    fun getFileWithBoundDir(context: Context, filePath: String): File {
        return getFile(context, filePath, true)
    }

    fun getAppFilesDir(context: Context): String {
        return context.filesDir.absolutePath
    }

    fun getExternalStorageDir(): String? {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            Environment.getExternalStorageDirectory().absolutePath
        } else {
            null
        }
    }

    fun getFileInfo(context: Context, filePath: String): Map<String, Any> {
        val file = getFile(context, filePath)
        if (!file.exists()) {
            return emptyMap()
        }
        return mapOf(
            "name" to file.name,
            "path" to file.absolutePath,
            "size" to file.length(),
            "lastModified" to file.lastModified(),
            "isDirectory" to file.isDirectory
        )
    }

    fun fileToBase64(context: Context, filePath: String): String? {
        return if (filePath.startsWith("content://")) {
            uriToBase64(context, Uri.parse(filePath))
        } else {
            val file = getFile(context, filePath)
            if (!file.exists()) {
                return null
            }
            try {
                Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
        } catch (e: Exception) {
            null
        }
    }

    fun readUriContent(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val content = inputStream?.reader()?.readText() ?: ""
        inputStream?.close()
        return content
    }

    fun readUriOrFile(context: Context, pathOrUri: String): String {
        return if (pathOrUri.startsWith("content://")) {
            readUriContent(context, Uri.parse(pathOrUri))
        } else {
            readFile(context, pathOrUri)
        }
    }

    fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "txt" -> "text/plain"
            "md" -> "text/markdown"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "html" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            else -> "application/octet-stream"
        }
    }

    fun getAttachmentType(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("audio/") -> "audio"
            mimeType.startsWith("text/") -> "text"
            else -> "file"
        }
    }
}
