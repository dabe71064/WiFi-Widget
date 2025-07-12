package com.w2sv.wifiwidget.ui.screens.home.components.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.isGranted
import com.w2sv.androidutils.isLocationEnabledCompat
import com.w2sv.composed.CollectLatestFromFlow
import com.w2sv.core.common.R
import com.w2sv.wifiwidget.ui.LocalLocationManager
import com.w2sv.wifiwidget.ui.designsystem.AppSnackbarVisuals
import com.w2sv.wifiwidget.ui.designsystem.ElevatedIconHeaderCard
import com.w2sv.wifiwidget.ui.designsystem.IconHeaderProperties
import com.w2sv.wifiwidget.ui.designsystem.SnackbarAction
import com.w2sv.wifiwidget.ui.designsystem.SnackbarKind
import com.w2sv.wifiwidget.ui.navigation.LocalNavigator
import com.w2sv.wifiwidget.ui.navigation.Navigator
import com.w2sv.wifiwidget.ui.screens.home.components.TriggerWidgetDataRefresh
import com.w2sv.wifiwidget.ui.sharedviewmodel.WidgetViewModel
import com.w2sv.wifiwidget.ui.states.LocationAccessState
import com.w2sv.wifiwidget.ui.utils.activityViewModel
import com.w2sv.wifiwidget.ui.utils.rememberSnackbarEmitter
import kotlinx.coroutines.flow.Flow

@Composable
fun WidgetCard(
    locationAccessState: LocationAccessState,
    modifier: Modifier = Modifier,
    widgetVM: WidgetViewModel = activityViewModel(),
    navigator: Navigator = LocalNavigator.current
) {
    val context = LocalContext.current

    ElevatedIconHeaderCard(
        iconHeaderProperties = remember {
            IconHeaderProperties(
                iconRes = R.drawable.ic_widgets_24,
                stringRes = R.string.widget
            )
        },
        modifier = modifier,
        content = {
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val buttonHeight = 60.dp
                PinWidgetButton(
                    onClick = { widgetVM.attemptWidgetPin(context) },
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .height(buttonHeight)
                )
                WidgetConfigurationButton(
                    onClick = { navigator.toWidgetConfiguration() },
                    modifier = Modifier.height(buttonHeight)
                )
            }
        }
    )

    ShowSnackbarOnWidgetPin(
        newWidgetPinned = widgetVM.widgetPinSuccessFlow,
        anyLocationAccessRequiringPropertyEnabled = { widgetVM.configuration.anyLocationAccessRequiringPropertyEnabled },
        locationAccessState = locationAccessState
    )
}

/**
 * Shows Snackbar on collection from [newWidgetPinned].
 */
@Composable
private fun ShowSnackbarOnWidgetPin(
    newWidgetPinned: Flow<Unit>,
    anyLocationAccessRequiringPropertyEnabled: () -> Boolean,
    locationAccessState: LocationAccessState
) {
    val snackbarEmitter = rememberSnackbarEmitter()
    val locationManager = LocalLocationManager.current

    CollectLatestFromFlow(newWidgetPinned) {
        if (anyLocationAccessRequiringPropertyEnabled()) {
            when {
                // Warn about (B)SSID not being displayed if device GPS is disabled
                !locationManager.isLocationEnabledCompat() -> snackbarEmitter.dismissCurrentAndShowSuspending {
                    AppSnackbarVisuals(
                        msg = getString(R.string.on_pin_widget_wo_gps_enabled),
                        kind = SnackbarKind.Warning
                    )
                }

                !locationAccessState.allPermissionsGranted -> snackbarEmitter.dismissCurrentAndShowSuspending {
                    AppSnackbarVisuals(
                        msg = getString(R.string.on_pin_widget_wo_location_access_permission),
                        kind = SnackbarKind.Warning,
                        action = SnackbarAction(
                            label = getString(R.string.grant),
                            callback = {
                                locationAccessState.launchMultiplePermissionRequest(
                                    TriggerWidgetDataRefresh,
                                    skipSnackbarIfInAppPromptingSuppressed = true
                                )
                            }
                        )
                    )
                }

                // Warn about (B)SSID not being reliably displayed if background location access not granted
                locationAccessState.backgroundAccessState?.status?.isGranted == false -> snackbarEmitter.dismissCurrentAndShowSuspending {
                    AppSnackbarVisuals(
                        msg = getString(R.string.on_pin_widget_wo_background_location_access_permission),
                        kind = SnackbarKind.Warning,
                        action = SnackbarAction(
                            label = getString(R.string.grant),
                            callback = {
                                locationAccessState.backgroundAccessState.launchPermissionRequest()
                            }
                        )
                    )
                }
            }
        }
        snackbarEmitter.dismissCurrentAndShowSuspending {
            AppSnackbarVisuals(
                msg = getString(R.string.pinned_widget),
                kind = SnackbarKind.Success
            )
        }
    }
}

@Composable
private fun PinWidgetButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.pin),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun WidgetConfigurationButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null
            )
            Text(stringResource(R.string.configure))
        }
    }
}
