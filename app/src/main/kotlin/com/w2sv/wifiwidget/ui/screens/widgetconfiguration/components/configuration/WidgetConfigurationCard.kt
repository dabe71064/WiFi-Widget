package com.w2sv.wifiwidget.ui.screens.widgetconfiguration.components.configuration

import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.w2sv.common.utils.moveElement
import com.w2sv.composed.extensions.thenIf
import com.w2sv.domain.model.WidgetBottomBarElement
import com.w2sv.domain.model.WidgetRefreshingParameter
import com.w2sv.domain.model.WifiProperty
import com.w2sv.wifiwidget.R
import com.w2sv.wifiwidget.ui.designsystem.AppSnackbarVisuals
import com.w2sv.wifiwidget.ui.designsystem.IconHeaderProperties
import com.w2sv.wifiwidget.ui.designsystem.SnackbarKind
import com.w2sv.wifiwidget.ui.designsystem.rememberShowSnackbar
import com.w2sv.wifiwidget.ui.screens.home.components.LocationAccessPermissionRequestTrigger
import com.w2sv.wifiwidget.ui.screens.widgetconfiguration.components.dialog.model.ColorPickerDialogData
import com.w2sv.wifiwidget.ui.screens.widgetconfiguration.components.dialog.model.InfoDialogData
import com.w2sv.wifiwidget.ui.screens.widgetconfiguration.components.dialog.model.infoDialogData
import com.w2sv.wifiwidget.ui.screens.widgetconfiguration.model.ReversibleWidgetConfiguration
import com.w2sv.wifiwidget.ui.states.LocationAccessState
import com.w2sv.wifiwidget.ui.utils.ShakeConfig
import com.w2sv.wifiwidget.ui.utils.ShakeController
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val checkRowColumnBottomPadding = 8.dp

@Immutable
data class WidgetConfigurationCard(
    val iconHeaderProperties: IconHeaderProperties,
    val content: @Composable () -> Unit
)

@Composable
fun rememberWidgetConfigurationCardProperties(
    widgetConfiguration: ReversibleWidgetConfiguration,
    locationAccessState: LocationAccessState,
    showInfoDialog: (InfoDialogData) -> Unit,
    showCustomColorConfigurationDialog: (ColorPickerDialogData) -> Unit,
    showRefreshIntervalConfigurationDialog: () -> Unit
): ImmutableList<WidgetConfigurationCard> {
    return remember {
        persistentListOf(
            WidgetConfigurationCard(
                iconHeaderProperties = IconHeaderProperties(
                    iconRes = R.drawable.ic_palette_24,
                    stringRes = R.string.appearance
                )
            ) {
                AppearanceConfiguration(
                    coloringConfig = widgetConfiguration.coloringConfig.collectAsStateWithLifecycle().value,
                    setColoringConfig = remember {
                        {
                            widgetConfiguration.coloringConfig.value = it
                        }
                    },
                    opacity = widgetConfiguration.opacity.collectAsStateWithLifecycle().value,
                    setOpacity = remember {
                        {
                            widgetConfiguration.opacity.value = it
                        }
                    },
                    fontSize = widgetConfiguration.fontSize.collectAsStateWithLifecycle().value,
                    setFontSize = remember {
                        { widgetConfiguration.fontSize.value = it }
                    },
                    showCustomColorConfigurationDialog = showCustomColorConfigurationDialog,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                )
            },
            WidgetConfigurationCard(
                IconHeaderProperties(
                    iconRes = R.drawable.ic_checklist_24,
                    stringRes = R.string.properties
                )
            ) {
                DragAndDroppableCheckRowColumn(
                    elements = rememberWidgetWifiPropertyCheckRowData(
                        widgetConfiguration = widgetConfiguration,
                        locationAccessState = locationAccessState,
                        showInfoDialog = showInfoDialog
                    ),
                    onDrop = { fromIndex: Int, toIndex: Int ->
                        widgetConfiguration
                            .orderedWifiProperties
                            .update {
                                it
                                    .toMutableList()
                                    .apply { moveElement(fromIndex, toIndex) }
                            }
                    }
                )
            },
            WidgetConfigurationCard(
                iconHeaderProperties = IconHeaderProperties(
                    iconRes = R.drawable.ic_bottom_row_24,
                    stringRes = R.string.bottom_bar
                )
            ) {
                CheckRowColumn(
                    elements = remember {
                        WidgetBottomBarElement.entries.map {
                            CheckRowColumnElement.CheckRow.fromIsCheckedMap(
                                property = it,
                                isCheckedMap = widgetConfiguration.bottomRowMap
                            )
                        }
                            .toPersistentList()
                    },
                    modifier = Modifier.padding(bottom = checkRowColumnBottomPadding)
                )
            },
            WidgetConfigurationCard(
                iconHeaderProperties = IconHeaderProperties(
                    iconRes = com.w2sv.core.common.R.drawable.ic_refresh_24,
                    stringRes = R.string.refreshing
                )
            ) {
                CheckRowColumn(
                    elements = remember {
                        persistentListOf(
                            CheckRowColumnElement.CheckRow.fromIsCheckedMap(
                                property = WidgetRefreshingParameter.RefreshPeriodically,
                                isCheckedMap = widgetConfiguration.refreshingParametersMap,
                                showInfoDialog = {
                                    showInfoDialog(
                                        InfoDialogData(
                                            titleRes = WidgetRefreshingParameter.RefreshPeriodically.labelRes,
                                            descriptionRes = R.string.refresh_periodically_info
                                        )
                                    )
                                },
                                subPropertyColumnElements = persistentListOf(
                                    CheckRowColumnElement.Custom {
                                        RefreshIntervalConfigurationRow(
                                            interval = widgetConfiguration.refreshInterval.collectAsStateWithLifecycle().value,
                                            showConfigurationDialog = showRefreshIntervalConfigurationDialog,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        )
                                    },
                                    CheckRowColumnElement.CheckRow.fromIsCheckedMap(
                                        property = WidgetRefreshingParameter.RefreshOnLowBattery,
                                        isCheckedMap = widgetConfiguration.refreshingParametersMap
                                    )
                                )
                            )
                        )
                    },
                    modifier = Modifier.padding(bottom = checkRowColumnBottomPadding)
                )
            }
        )
    }
}

@Composable
private fun rememberWidgetWifiPropertyCheckRowData(
    widgetConfiguration: ReversibleWidgetConfiguration,
    locationAccessState: LocationAccessState,
    showInfoDialog: (InfoDialogData) -> Unit
): ImmutableList<CheckRowColumnElement.CheckRow<WifiProperty>> {
    val context = LocalContext.current
    val scope: CoroutineScope = rememberCoroutineScope()

    val showLeaveAtLeastOnePropertyEnabledSnackbar: () -> Unit = rememberShowSnackbar {
        AppSnackbarVisuals(
            msg = context.getString(R.string.leave_at_least_one_property_enabled),
            kind = SnackbarKind.Warning
        )
    }

    val showLeaveAtLeastOneAddressVersionEnabledSnackbar: () -> Unit = rememberShowSnackbar {
        AppSnackbarVisuals(
            msg = context.getString(R.string.leave_at_least_one_address_version_enabled),
            kind = SnackbarKind.Warning
        )
    }

    val orderedWifiProperties by widgetConfiguration.orderedWifiProperties.collectAsStateWithLifecycle()
    return remember(orderedWifiProperties) {
        orderedWifiProperties
            .map { property ->
                property.checkRow(
                    widgetConfiguration = widgetConfiguration,
                    locationAccessState = locationAccessState,
                    showInfoDialog = showInfoDialog,
                    showLeaveAtLeastOnePropertyEnabledSnackbar = showLeaveAtLeastOnePropertyEnabledSnackbar,
                    showLeaveAtLeastOneAddressVersionEnabledSnackbar = showLeaveAtLeastOneAddressVersionEnabledSnackbar,
                    scope = scope
                )
            }
            .toPersistentList()
    }
}

private fun WifiProperty.checkRow(
    widgetConfiguration: ReversibleWidgetConfiguration,
    locationAccessState: LocationAccessState,
    showInfoDialog: (InfoDialogData) -> Unit,
    showLeaveAtLeastOnePropertyEnabledSnackbar: () -> Unit,
    showLeaveAtLeastOneAddressVersionEnabledSnackbar: () -> Unit,
    scope: CoroutineScope
): CheckRowColumnElement.CheckRow<WifiProperty> {
    val shakeController = ShakeController(shakeConfig)

    return CheckRowColumnElement.CheckRow.fromIsCheckedMap(
        property = this,
        isCheckedMap = widgetConfiguration.wifiProperties,
        allowCheckChange = { isCheckedNew ->
            if (this is WifiProperty.NonIP.LocationAccessRequiring && isCheckedNew) {
                return@fromIsCheckedMap locationAccessState.isGranted.also {
                    if (!it) {
                        locationAccessState.launchRequest(
                            LocationAccessPermissionRequestTrigger.PropertyCheckChange(
                                this
                            )
                        )
                    }
                }
            }
            (isCheckedNew || widgetConfiguration.moreThanOnePropertyChecked()).also {
                if (!it) {
                    showLeaveAtLeastOnePropertyEnabledSnackbar()
                }
            }
        },
        onCheckedChangedDisallowed = { scope.launch { shakeController.shake() } },
        shakeController = shakeController,
        subPropertyColumnElements = when (this) {
            is WifiProperty.IP ->
                subPropertyElements(
                    ipSubPropertyEnablementMap = widgetConfiguration.ipSubProperties,
                    showLeaveAtLeastOneAddressVersionEnabledSnackbar = showLeaveAtLeastOneAddressVersionEnabledSnackbar,
                    scope = scope
                )

            else -> null
        },
        showInfoDialog = { showInfoDialog(infoDialogData()) }
    )
}

private fun ReversibleWidgetConfiguration.moreThanOnePropertyChecked(): Boolean =
    wifiProperties.values.count { it } > 1

private fun WifiProperty.IP.subPropertyElements(
    ipSubPropertyEnablementMap: MutableMap<WifiProperty.IP.SubProperty, Boolean>,
    showLeaveAtLeastOneAddressVersionEnabledSnackbar: () -> Unit,
    scope: CoroutineScope
): ImmutableList<CheckRowColumnElement> {
    return buildList {
        if (this@subPropertyElements is WifiProperty.IP.V4AndV6) {
            add(
                CheckRowColumnElement.Custom {
                    VersionsHeader()
                }
            )
        }
        addAll(
            subProperties
                .map { subProperty ->
                    val shakeController =
                        if (subProperty.isAddressTypeEnablementProperty) {
                            ShakeController(shakeConfig)
                        } else {
                            null
                        }

                    CheckRowColumnElement.CheckRow.fromIsCheckedMap(
                        property = subProperty,
                        isCheckedMap = ipSubPropertyEnablementMap,
                        allowCheckChange = { newValue ->
                            subProperty.allowCheckedChange(
                                newValue,
                                ipSubPropertyEnablementMap
                            )
                        },
                        onCheckedChangedDisallowed = {
                            scope.launch { shakeController?.shake() }
                            showLeaveAtLeastOneAddressVersionEnabledSnackbar()
                        },
                        shakeController = shakeController,
                        modifier = Modifier
                            .thenIf(
                                condition = subProperty.isAddressTypeEnablementProperty,
                                onTrue = { padding(start = 8.dp) }
                            )
                    )
                }
        )
    }
        .toPersistentList()
}

private val shakeConfig = ShakeConfig(
    iterations = 2,
    translateX = 20f,
    stiffness = Spring.StiffnessHigh
)

private fun WifiProperty.IP.SubProperty.allowCheckedChange(
    newValue: Boolean,
    subPropertyEnablementMap: Map<WifiProperty.IP.SubProperty, Boolean>
): Boolean =
    when (val capturedKind = kind) {
        is WifiProperty.IP.V4AndV6.AddressTypeEnablement -> {
            newValue || subPropertyEnablementMap.getValue(
                WifiProperty.IP.SubProperty(
                    property = property,
                    kind = capturedKind.opposingAddressTypeEnablement
                )
            )
        }

        else -> true
    }
