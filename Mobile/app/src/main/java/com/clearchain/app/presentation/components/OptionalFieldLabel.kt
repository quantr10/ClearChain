package com.clearchain.app.presentation.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val OptionalSuffixRegex = Regex("\\s*\\((?i:optional)\\)")

@Composable
fun OptionalFieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    isOptional: Boolean = true,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    val baseText = text.replace(OptionalSuffixRegex, "").trim()
    Text(
        text = buildAnnotatedString {
            append(baseText)
            if (isOptional) {
                append(" ")
                withStyle(
                    SpanStyle(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal
                    )
                ) {
                    append("(optional)")
                }
            }
        },
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}
