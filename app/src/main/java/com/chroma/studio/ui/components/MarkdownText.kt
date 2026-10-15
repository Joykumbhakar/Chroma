package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    defaultColor: Color = Color.White,
    textAlign: TextAlign = TextAlign.Start
) {
    val annotatedString = buildAnnotatedString {
        // Simple regex-based markdown parser
        // We will process line by line for paragraph/headings, and then apply span styles.
        
        val lines = markdown.split("\n")
        
        for ((index, line) in lines.withIndex()) {
            val startIndex = length
            var currentLine = line
            
            // Check for headings
            var headingLevel = 0
            val headingRegex = Regex("""^#{1,6}\s+""")
            val headingMatch = headingRegex.find(currentLine)
            if (headingMatch != null) {
                headingLevel = headingMatch.value.trim().length
                currentLine = currentLine.substring(headingMatch.value.length)
            }
            
            append(currentLine)
            
            // Apply paragraph style (heading size)
            val fontSize = when (headingLevel) {
                1 -> 32.sp
                2 -> 28.sp
                3 -> 24.sp
                4 -> 20.sp
                5 -> 18.sp
                6 -> 16.sp
                else -> 15.sp
            }
            val fontWeight = if (headingLevel > 0) FontWeight.Bold else FontWeight.Normal
            
            addStyle(
                style = ParagraphStyle(textAlign = textAlign),
                start = startIndex,
                end = length
            )
            
            addStyle(
                style = SpanStyle(fontSize = fontSize, fontWeight = fontWeight, color = defaultColor),
                start = startIndex,
                end = length
            )
            
            if (index < lines.size - 1) {
                append("\n")
            }
        }
        
        // Apply inline styles (Bold, Italic, Underline, Highlight, Color)
        val currentAnnotated = toAnnotatedString()
        val rawStr = currentAnnotated.text
        
        // Bold: **text**
        val boldRegex = Regex("""\*\*(.*?)\*\*""")
        boldRegex.findAll(rawStr).forEach { result ->
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), result.range.first, result.range.last + 1)
        }
        
        // Italic: *text*
        val italicRegex = Regex("""(?<!\*)\*(?!\*)(.*?)(?<!\*)\*(?!\*)""")
        italicRegex.findAll(rawStr).forEach { result ->
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), result.range.first, result.range.last + 1)
        }
        
        // Underline: __text__
        val underlineRegex = Regex("""__(.*?)__""")
        underlineRegex.findAll(rawStr).forEach { result ->
            addStyle(SpanStyle(textDecoration = TextDecoration.Underline), result.range.first, result.range.last + 1)
        }
        
        // Highlight: ==text==
        val highlightRegex = Regex("""==(.*?)==""")
        highlightRegex.findAll(rawStr).forEach { result ->
            addStyle(SpanStyle(background = Color(0x66FFFF00)), result.range.first, result.range.last + 1)
        }
        
        // Color: [color:#RRGGBB]text[/color]
        val colorRegex = Regex("""\[color:(#[0-9A-Fa-f]{6,8})\](.*?)\[/color\]""")
        colorRegex.findAll(rawStr).forEach { result ->
            try {
                val hex = result.groupValues[1]
                val color = Color(android.graphics.Color.parseColor(hex))
                addStyle(SpanStyle(color = color), result.range.first, result.range.last + 1)
            } catch (e: Exception) { }
        }
        
        // We actually want to hide the markdown characters. 
        // For a true markdown parser we wouldn't append them in the first place, 
        // but for a lightweight AnnotatedString builder it's easier to make them transparent and 0.sp.
        val hideStyle = SpanStyle(color = Color.Transparent, fontSize = 1.sp)
        
        boldRegex.findAll(rawStr).forEach { 
            addStyle(hideStyle, it.range.first, it.range.first + 2)
            addStyle(hideStyle, it.range.last - 1, it.range.last + 1)
        }
        italicRegex.findAll(rawStr).forEach { 
            addStyle(hideStyle, it.range.first, it.range.first + 1)
            addStyle(hideStyle, it.range.last, it.range.last + 1)
        }
        underlineRegex.findAll(rawStr).forEach { 
            addStyle(hideStyle, it.range.first, it.range.first + 2)
            addStyle(hideStyle, it.range.last - 1, it.range.last + 1)
        }
        highlightRegex.findAll(rawStr).forEach { 
            addStyle(hideStyle, it.range.first, it.range.first + 2)
            addStyle(hideStyle, it.range.last - 1, it.range.last + 1)
        }
        colorRegex.findAll(rawStr).forEach {
            val startTag = it.groups[1]!!.range.first - 7 // "[color:" length
            val endStartTag = it.groups[1]!!.range.last + 1 // "]" length
            addStyle(hideStyle, startTag, endStartTag + 1)
            val endTag = it.range.last - 7 // "[/color]" length
            addStyle(hideStyle, endTag, it.range.last + 1)
        }
    }
    
    BasicText(
        text = annotatedString,
        modifier = modifier
    )
}
