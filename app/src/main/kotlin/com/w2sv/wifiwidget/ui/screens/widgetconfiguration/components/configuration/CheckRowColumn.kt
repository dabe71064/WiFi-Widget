package com.w2sv.wifiwidget.ui.screens.widgetconfiguration.components.configuration

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.w2sv.common.utils.onAtLeastAndroidR
import com.w2sv.common.utils.onAtLeastAndroidU
import com.w2sv.composed.extensions.thenIf
import com.w2sv.composed.extensions.thenIfNotNull
import com.w2sv.wifiwidget.R
import com.w2sv.wifiwidget.ui.designsystem.InfoIcon
import com.w2sv.wifiwidget.ui.designsystem.KeyboardArrowRightIcon
import com.w2sv.wifiwidget.ui.designsystem.biggerIconSize
import com.w2sv.wifiwidget.ui.designsystem.nestedContentBackground
import com.w2sv.wifiwidget.ui.utils.alphaDecreased
import com.w2sv.wifiwidget.ui.utils.shake
import kotlinx.collections.immutable.ImmutableList
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * For alignment of primary check row click elements with sub property click elements
 */
private val primaryCheckRowModifier = Modifier.padding(end = 16.dp)

@Composable
fun CheckRowColumn(
    elements: ImmutableList<CheckRowColumnElement.CheckRow<*>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        elements
            .forEach { data ->
                when (data.hasSubProperties) {
                    false -> {
                        CheckRow(data = data, modifier = primaryCheckRowModifier)
                    }

                    true -> {
                        CheckRowWithSubProperties(
                            data = data
                        )
                    }
                }
            }
    }
}

@Composable
fun DragAndDroppableCheckRowColumn(
    elements: ImmutableList<CheckRowColumnElement.CheckRow<*>>,
    modifier: Modifier = Modifier,
    onDrop: (fromIndex: Int, toIndex: Int) -> Unit
) {
    val view = LocalView.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onDrop(from.index, to.index)
        onAtLeastAndroidU { view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK) }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.heightIn(max = 2_000.dp),  // Max height necessary due to nesting inside scrollable column. Chosen arbitrarily ("some height that accommodates the column height")
        userScrollEnabled = false
    ) {
        items(elements, key = { it.property.labelRes }) { data ->
            ReorderableItem(reorderableLazyListState, key = data.property.labelRes) { isDragging ->
                val itemModifier = Modifier
                    .longPressDraggableHandle(
                        onDragStarted = {
                            onAtLeastAndroidR { view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START) }
                        },
                        onDragStopped = {
                            onAtLeastAndroidR { view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END) }
                        }
                    )
                    .thenIf(isDragging) {
                        val shadowColor = MaterialTheme.colorScheme.secondary.alphaDecreased()
                        shadow(
                            elevation = 1.dp,
                            shape = CircleShape,
                            ambientColor = shadowColor,
                            spotColor = shadowColor
                        )
                    }

                when (data.hasSubProperties) {
                    false -> {
                        CheckRow(
                            data = data,
                            modifier = primaryCheckRowModifier.then(itemModifier)
                        )
                    }

                    true -> {
                        CheckRowWithSubProperties(
                            data = data,
                            modifier = itemModifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(data: CheckRowColumnElement.CheckRow<*>, modifier: Modifier = Modifier) {
    CheckRowBase(
        data = data,
        leadingIcon = {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                KeyboardArrowRightIcon(tint = data.leadingIconAndLabelColor)
            }
        },
        modifier = modifier,
        labelColor = data.leadingIconAndLabelColor
    )
}

@Composable
private fun CheckRowWithSubProperties(data: CheckRowColumnElement.CheckRow<*>, modifier: Modifier = Modifier) {
    var expandSubProperties by rememberSaveable {
        mutableStateOf(false)
    }
    // Collapse subProperties on unchecking
    LaunchedEffect(data, data.isChecked()) {
        if (!data.isChecked()) {
            expandSubProperties = false
        }
    }

    Column(modifier = modifier) {
        CheckRowBase(
            data = data,
            leadingIcon = {
                IconButton(
                    onClick = remember {
                        {
                            expandSubProperties = !expandSubProperties
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    enabled = data.isChecked()
                ) {
                    Icon(
                        imageVector = if (expandSubProperties) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            },
            modifier = primaryCheckRowModifier,
            labelColor = data.leadingIconAndLabelColor
        )

        AnimatedVisibility(visible = expandSubProperties) {
            SubPropertyCheckRowColumn(elements = data.subPropertyColumnElements!!)
        }
    }
}

@Composable
private fun SubPropertyCheckRowColumn(elements: ImmutableList<CheckRowColumnElement>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(start = 24.dp) // Make background start at the indentation of CheckRowBase label
            .nestedContentBackground()
            .padding(start = subPropertyColumnPadding)
    ) {
        elements.forEach { view ->
            when (view) {
                is CheckRowColumnElement.CheckRow<*> -> {
                    CheckRowBase(
                        data = view,
                        modifier = view.modifier,
                        fontSize = subPropertyCheckRowColumnFontSize,
                        leadingIcon = {
                            SubPropertyKeyboardArrowRightIcon()
                        }
                    )
                }

                is CheckRowColumnElement.Custom -> {
                    view.content()
                }
            }
        }
    }
}

@Composable
fun VersionsHeader(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.versions),
        modifier = modifier.padding(top = subPropertyColumnPadding),
        fontSize = subPropertyCheckRowColumnFontSize,
        fontWeight = FontWeight.SemiBold
    )
}

private val subPropertyCheckRowColumnFontSize = 14.sp
private val subPropertyColumnPadding = 12.dp

@Composable
private fun CheckRowBase(
    data: CheckRowColumnElement.CheckRow<*>,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    labelColor: Color = MaterialTheme.colorScheme.onBackground,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val label = stringResource(id = data.property.labelRes)
    val checkBoxCD = stringResource(id = R.string.set_unset, label)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(data.modifier)
            .thenIfNotNull(data.shakeController) {
                shake(it)
            }
    ) {
        leadingIcon?.invoke()
        Text(
            text = label,
            fontSize = fontSize,
            color = labelColor,
            modifier = Modifier.weight(1.0f)
        )
        data.showInfoDialog?.let {
            InfoIconButton(
                onClick = { it() },
                contentDescription = stringResource(id = R.string.info_icon_cd, label)
            )
        }
        Checkbox(
            checked = data.isChecked(),
            onCheckedChange = {
                data.onCheckedChange(it)
            },
            modifier = Modifier.semantics {
                contentDescription = checkBoxCD
            }
        )
    }
}

@Composable
private fun InfoIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        InfoIcon(
            contentDescription = contentDescription,
            modifier = Modifier.size(biggerIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
