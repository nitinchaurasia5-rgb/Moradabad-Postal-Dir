package com.example.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.theme.HighlightBgDark
import com.example.ui.theme.HighlightBgLight
import com.example.ui.theme.HighlightTextDark
import com.example.ui.theme.HighlightTextLight

/**
 * Builds an AnnotatedString that highlights all occurrences of [query] within [text].
 */
fun buildHighlightedText(
    text: String,
    query: String,
    highlightBg: Color,
    highlightColor: Color
): AnnotatedString {
    if (query.isBlank()) {
        return AnnotatedString(text)
    }

    val lowerText = text.lowercase()
    val lowerQuery = query.trim().lowercase()
    val matches = mutableListOf<IntRange>()

    var startIndex = 0
    while (startIndex < lowerText.length) {
        val found = lowerText.indexOf(lowerQuery, startIndex)
        if (found == -1) break
        matches.add(found until (found + lowerQuery.length))
        startIndex = found + lowerQuery.length
    }

    if (matches.isEmpty()) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        append(text)
        for (range in matches) {
            addStyle(
                style = SpanStyle(
                    background = highlightBg,
                    color = highlightColor,
                    fontWeight = FontWeight.Bold
                ),
                start = range.first,
                end = range.last + 1
            )
        }
    }
}

/**
 * Composable that displays text with real-time highlighted search terms.
 */
@Composable
fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val isDark = isSystemInDarkTheme()
    val highlightBg = if (isDark) HighlightBgDark else HighlightBgLight
    val highlightText = if (isDark) HighlightTextDark else HighlightTextLight

    val annotated = buildHighlightedText(
        text = text,
        query = query,
        highlightBg = highlightBg,
        highlightColor = highlightText
    )

    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow
    )
}
