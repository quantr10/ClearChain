package com.clearchain.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.clearchain.app.R
import com.clearchain.app.ui.theme.Alpha
import com.clearchain.app.ui.theme.BorderWidth
import com.clearchain.app.ui.theme.ButtonShape
import com.clearchain.app.ui.theme.CodeCellMinHeight
import com.clearchain.app.ui.theme.ListGap

/**
 * A one-time code entered as [length] cells. Underneath it's one text field, so a screen reader
 * announces "[label], 6 digits" and how many are entered, never six separate fields. It keeps
 * digits only, so a pasted "482 913" or "482-913" fills the cells as "482913". It never submits on
 * its own: the caller's [imeAction] does.
 */
@Composable
fun CodeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    length: Int = 6,
    showLabel: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val fieldDescription = pluralStringResource(R.plurals.cd_code_field, length, label, length)
    val progress = pluralStringResource(R.plurals.cd_code_digits_entered, value.length, value.length)
    val digitStyle = MaterialTheme.typography.headlineSmall
    val caretHeight = with(LocalDensity.current) { digitStyle.lineHeight.toDp() }

    Column(
        modifier = modifier.then(if (enabled) Modifier else Modifier.alpha(Alpha.Disabled)),
        verticalArrangement = Arrangement.spacedBy(ListGap)
    ) {
        if (showLabel) {
            OptionalFieldLabel(
                text = label,
                isOptional = false,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(Char::isDigit).take(length)) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = fieldDescription
                    stateDescription = progress
                    contentType = ContentType.SmsOtpCode
                    if (isError && !errorMessage.isNullOrBlank()) error(errorMessage)
                },
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clearAndSetSemantics {},
                        horizontalArrangement = Arrangement.spacedBy(ListGap)
                    ) {
                        repeat(length) { index ->
                            val digit = value.getOrNull(index)
                            val isNext = isFocused && index == value.length
                            val borderColor = when {
                                isError -> MaterialTheme.colorScheme.error
                                isNext -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = CodeCellMinHeight)
                                    .background(MaterialTheme.colorScheme.surface, ButtonShape)
                                    .border(BorderWidth, borderColor, ButtonShape),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    digit != null -> Text(
                                        text = digit.toString(),
                                        style = digitStyle,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    isNext -> Box(
                                        modifier = Modifier
                                            .width(BorderWidth * 2)
                                            .height(caretHeight)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                    // Kept composed so typing, pasting and the keyboard still work; it draws nothing.
                    Box(Modifier.matchParentSize()) { innerTextField() }
                }
            }
        )

        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
