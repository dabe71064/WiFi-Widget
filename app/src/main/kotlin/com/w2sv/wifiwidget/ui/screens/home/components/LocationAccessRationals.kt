package com.w2sv.wifiwidget.ui.screens.home.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.w2sv.composed.rememberStyledTextResource
import com.w2sv.core.common.R
import com.w2sv.domain.model.WifiProperty
import com.w2sv.wifiwidget.ui.designsystem.DialogButton
import com.w2sv.wifiwidget.ui.designsystem.InfoIcon
import com.w2sv.wifiwidget.ui.states.LocationAccessState

@Immutable
sealed interface LocationAccessPermissionOnGrantAction

@Immutable
data object EnableLocationAccessDependentProperties : LocationAccessPermissionOnGrantAction

@Immutable
data object TriggerWidgetDataRefresh : LocationAccessPermissionOnGrantAction

@Immutable
@JvmInline
value class EnablePropertyOnReversibleConfiguration(val property: WifiProperty.NonIP.LocationAccessRequiring) :
    LocationAccessPermissionOnGrantAction

@Immutable
sealed interface LocationAccessPermissionStatus {
    @Immutable
    data object NotGranted : LocationAccessPermissionStatus

    @Immutable
    @JvmInline
    value class Granted(val onGrantAction: LocationAccessPermissionOnGrantAction?) : LocationAccessPermissionStatus

    val grantedOrNull: Granted?
        get() = this as? Granted

    companion object {
        fun get(isGranted: Boolean): LocationAccessPermissionStatus =
            if (isGranted) Granted(null) else NotGranted
    }
}

@Composable
fun LocationAccessRationals(state: LocationAccessState) {
    val rationalShown by state.rationalShown.collectAsStateWithLifecycle()

    if (!rationalShown) {
        LocationAccessPermissionRational(onProceed = state::onRationalShown)
    }
    state.backgroundAccessState?.run {
        val showBackgroundAccessRational by showRational.collectAsStateWithLifecycle()
        if (showBackgroundAccessRational) {
            BackgroundLocationAccessRational(
                launchPermissionRequest = ::launchPermissionRequest,
                onDismissRequest = ::hideRational
            )
        }
    }
}

@Composable
private fun LocationAccessPermissionRational(onProceed: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        modifier = modifier,
        icon = { InfoIcon() },
        text = {
            Text(
                text = rememberStyledTextResource(id = R.string.location_access_permission_rational),
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            DialogButton(
                onClick = onProceed,
                modifier = Modifier.fillMaxWidth()
            ) { Text(text = stringResource(R.string.understood)) }
        },
        onDismissRequest = onProceed
    )
}

@Composable
private fun BackgroundLocationAccessRational(launchPermissionRequest: () -> Unit, onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            DialogButton(
                onClick = {
                    launchPermissionRequest()
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(id = R.string.grant))
            }
        },
        dismissButton = {
            DialogButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.maybe_later))
            }
        },
        icon = { InfoIcon() },
        text = { Text(text = rememberStyledTextResource(id = R.string.background_location_access_rational)) }
    )
}
