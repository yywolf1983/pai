package dev.jeziellago.compose.markdowntext

import android.content.Context
import android.os.Build
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import androidx.annotation.FontRes
import androidx.annotation.IdRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.TextViewCompat
import coil3.ImageLoader
import io.noties.markwon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Pattern

private val TABLE_CELL_PATTERN = Pattern.compile("\\|")
private val TABLE_SEPARATOR_PATTERN = Pattern.compile("^\\s*\\|[-:|\\s]+\\|\\s*$")

private fun containsTable(markdown: String): Boolean {
    val lines = markdown.split("\n")
    var hasHeader = false
    var hasSeparator = false
    
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        
        if (TABLE_SEPARATOR_PATTERN.matcher(trimmed).matches()) {
            if (hasHeader) {
                hasSeparator = true
                break
            }
        } else if (TABLE_CELL_PATTERN.matcher(trimmed).find()) {
            hasHeader = true
        }
    }
    
    return hasHeader && hasSeparator
}

private fun isTableComplete(markdown: String): Boolean {
    if (!containsTable(markdown)) {
        return true
    }
    
    val lines = markdown.split("\n")
    var dataRowCount = 0
    var hasSeparator = false
    
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        
        if (TABLE_SEPARATOR_PATTERN.matcher(trimmed).matches()) {
            hasSeparator = true
        } else if (hasSeparator && TABLE_CELL_PATTERN.matcher(trimmed).find()) {
            dataRowCount++
        }
    }
    
    if (dataRowCount >= 2) {
        return true
    }
    
    if (lines.size >= 6) {
        val lastLine = lines.lastOrNull() ?: ""
        return lastLine.trim().isEmpty() || !TABLE_CELL_PATTERN.matcher(lastLine).find()
    }
    
    return false
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    linkColor: Color = Color.Unspecified,
    truncateOnTextOverflow: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    isTextSelectable: Boolean = false,
    textSelectionColors: TextSelectionColors? = null,
    autoSizeConfig: AutoSizeConfig? = null,
    @FontRes fontResource: Int? = null,
    style: TextStyle = LocalTextStyle.current,
    @IdRes viewId: Int? = null,
    onClick: (() -> Unit)? = null,
    disableLinkMovementMethod: Boolean = false,
    imageLoader: ImageLoader? = null,
    linkifyMask: Int = Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS or Linkify.WEB_URLS,
    enableSoftBreakAddsNewLine: Boolean = false,
    syntaxHighlightColor: Color = Color.LightGray,
    syntaxHighlightTextColor: Color = Color.Unspecified,
    headingBreakColor: Color = Color.Transparent,
    enableUnderlineForLink: Boolean = true,
    importForAccessibility: Int = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO,
    wrapMultilineTextWidth: Boolean = false,
    beforeSetMarkdown: ((TextView, Spanned) -> Unit)? = null,
    afterSetMarkdown: ((TextView) -> Unit)? = null,
    onLinkClicked: ((String) -> Unit)? = null,
    onTextLayout: ((numLines: Int) -> Unit)? = null
) {
    val defaultColor: Color = LocalContentColor.current
    val context: Context = LocalContext.current
    val markdownRender: Markwon =
        remember {
            MarkdownRender.create(
                context,
                imageLoader,
                linkifyMask,
                enableSoftBreakAddsNewLine,
                syntaxHighlightColor,
                syntaxHighlightTextColor,
                headingBreakColor,
                enableUnderlineForLink,
                beforeSetMarkdown,
                afterSetMarkdown,
                onLinkClicked,
                style
            )
        }

    val androidViewModifier = if (onClick != null) {
        Modifier
            .clickable { onClick() }
            .then(modifier)
    } else {
        modifier
    }
    
    val scope = remember { CoroutineScope(Dispatchers.Main + SupervisorJob()) }
    val lastRenderedMarkdown = remember { mutableStateOf(markdown) }
    val isRendering = remember { mutableStateOf(false) }
    val pendingMarkdown = remember { mutableStateOf<String?>(null) }
    
    DisposableEffect(markdown) {
        if (markdown == lastRenderedMarkdown.value) {
            onDispose { }
            return@DisposableEffect
        }
        
        val hasTable = containsTable(markdown)
        val tableComplete = isTableComplete(markdown)
        
        val delayMs = if (hasTable && !tableComplete) 500L else 50L
        
        pendingMarkdown.value = markdown
        
        if (!isRendering.value) {
            isRendering.value = true
            
            scope.launch {
                delay(delayMs)
                
                val currentPending = pendingMarkdown.value
                if (currentPending != null) {
                    val currentHasTable = containsTable(currentPending)
                    val currentTableComplete = isTableComplete(currentPending)
                    
                    if (!currentHasTable || currentTableComplete) {
                        lastRenderedMarkdown.value = currentPending
                        pendingMarkdown.value = null
                    }
                }
                isRendering.value = false
            }
        } else {
            scope.launch {
                delay(delayMs)
                
                val currentPending = pendingMarkdown.value
                if (currentPending != null) {
                    val currentHasTable = containsTable(currentPending)
                    val currentTableComplete = isTableComplete(currentPending)
                    
                    if (!currentHasTable || currentTableComplete) {
                        lastRenderedMarkdown.value = currentPending
                        pendingMarkdown.value = null
                    }
                }
            }
        }
        
        onDispose {
            pendingMarkdown.value = null
        }
    }
    
    val contentKey = remember(lastRenderedMarkdown.value) {
        lastRenderedMarkdown.value.hashCode()
    }
    
    key(contentKey) {
        AndroidView(
            modifier = androidViewModifier,
            factory = { factoryContext ->

                val linkTextColor = linkColor.takeOrElse { style.color.takeOrElse { defaultColor } }

                CustomTextView(factoryContext).apply {
                    viewId?.let { id = viewId }
                    fontResource?.let { font -> applyFontResource(font) }
                    importantForAccessibility = importForAccessibility

                    setMaxLines(maxLines)
                    setLinkTextColor(linkTextColor.toArgb())
                    this.wrapMultilineTextWidth = wrapMultilineTextWidth
                    setTextIsSelectable(isTextSelectable)
                    movementMethod = LinkMovementMethod.getInstance()

                    if (truncateOnTextOverflow) enableTextOverflow()

                    autoSizeConfig?.let { config ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                                this,
                                config.autoSizeMinTextSize,
                                config.autoSizeMaxTextSize,
                                config.autoSizeStepGranularity,
                                config.unit
                            )
                        }
                    }
                }
            },
            update = { textView ->
                // 只有当内容真正变化时才重置状态，避免不必要的闪屏
                if (textView.text.toString() != lastRenderedMarkdown.value) {
                    // 只更新文本，不重置状态，避免闪屏
                    with(textView) {
                        applyTextColor(style.color.takeOrElse { defaultColor }.toArgb())
                        applyFontSize(style)
                        applyLineHeight(style)
                        applyTextDecoration(style)
                        textSelectionColors?.let { applyTextSelectionColors(it) }

                        with(style) {
                            applyTextAlign(textAlign)
                            fontFamily?.let { applyFontFamily(this) }
                            fontStyle?.let { applyFontStyle(it) }
                            fontWeight?.let { applyFontWeight(it) }
                        }
                    }
                    // 直接设置markdown，避免resetTextState
                    markdownRender.setMarkdown(textView, lastRenderedMarkdown.value)
                    if (disableLinkMovementMethod) {
                        textView.movementMethod = null
                    }
                    if (onTextLayout != null) {
                        textView.post {
                            onTextLayout(textView.lineCount)
                        }
                    }
                    textView.maxLines = maxLines
                }
            }
        )
    }
}
