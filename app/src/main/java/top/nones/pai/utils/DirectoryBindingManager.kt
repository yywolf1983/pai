package top.nones.pai.utils

import android.content.Context
import android.content.SharedPreferences

class DirectoryBindingManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("file_binding", Context.MODE_PRIVATE)
    private var boundDir: String? = null

    init {
        boundDir = prefs.getString("bound_dir", null)
    }

    fun bindDirectory(path: String): Boolean {
        try {
            val dirFile = java.io.File(path)
            if (!dirFile.exists()) {
                dirFile.mkdirs()
            }
            if (dirFile.exists() && dirFile.isDirectory) {
                boundDir = path
                prefs.edit().putString("bound_dir", path).apply()
                return true
            }
        } catch (e: Exception) {
        }
        return false
    }

    fun getBoundDirectory(): String? = boundDir

    fun unbind() {
        boundDir = null
        prefs.edit().remove("bound_dir").apply()
    }

    fun isBound(): Boolean = boundDir != null

    fun updateBoundDirectory(path: String): Boolean {
        return bindDirectory(path)
    }
}