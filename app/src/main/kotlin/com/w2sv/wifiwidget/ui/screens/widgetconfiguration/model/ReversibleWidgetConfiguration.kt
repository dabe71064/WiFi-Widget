package com.w2sv.wifiwidget.ui.screens.widgetconfiguration.model

import androidx.compose.runtime.Stable
import com.w2sv.domain.model.FontSize
import com.w2sv.domain.model.WidgetBottomBarElement
import com.w2sv.domain.model.WidgetColoring
import com.w2sv.domain.model.WidgetRefreshingParameter
import com.w2sv.domain.model.WifiProperty
import com.w2sv.reversiblestate.ReversibleStateFlow
import com.w2sv.reversiblestate.ReversibleStateMap
import com.w2sv.reversiblestate.ReversibleStatesComposition
import com.w2sv.wifiwidget.ui.screens.home.components.LocationAccessPermissionRequestTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration

@Stable
class ReversibleWidgetConfiguration(
    val coloringConfig: ReversibleStateFlow<WidgetColoring.Config>,
    val opacity: ReversibleStateFlow<Float>,
    val fontSize: ReversibleStateFlow<FontSize>,
    val wifiProperties: ReversibleStateMap<WifiProperty, Boolean>,
    val orderedWifiProperties: ReversibleStateFlow<List<WifiProperty>>,
    val ipSubProperties: ReversibleStateMap<WifiProperty.IP.SubProperty, Boolean>,
    val bottomRowMap: ReversibleStateMap<WidgetBottomBarElement, Boolean>,
    val refreshInterval: ReversibleStateFlow<Duration>,
    val refreshingParametersMap: ReversibleStateMap<WidgetRefreshingParameter, Boolean>,
    private val scope: CoroutineScope,
    onStateSynced: suspend () -> Unit
) : ReversibleStatesComposition(
    reversibleStates = listOf(
        coloringConfig,
        opacity,
        fontSize,
        wifiProperties,
        orderedWifiProperties,
        ipSubProperties,
        bottomRowMap,
        refreshInterval,
        refreshingParametersMap
    ),
    scope = scope,
    onStateSynced = { onStateSynced() }
) {
    val anyLocationAccessRequiringPropertyEnabled: Boolean
        get() = WifiProperty.NonIP.LocationAccessRequiring.entries
            .any {
                wifiProperties.appliedStateMap.getValue(it).value
            }

    fun onLocationAccessPermissionGranted(trigger: LocationAccessPermissionRequestTrigger?) {
        when (trigger) {
            is LocationAccessPermissionRequestTrigger.InitialAppLaunch -> {
                WifiProperty.NonIP.LocationAccessRequiring.entries.forEach {
                    wifiProperties[it] = true
                }
                scope.launch { wifiProperties.sync() }
            }

            is LocationAccessPermissionRequestTrigger.PropertyCheckChange -> {
                wifiProperties[trigger.property] = true
            }

            else -> Unit
        }
    }
}
