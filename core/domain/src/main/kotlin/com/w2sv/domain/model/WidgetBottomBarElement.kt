package com.w2sv.domain.model

import androidx.annotation.StringRes
import com.w2sv.core.domain.R

enum class WidgetBottomBarElement(
    @param:StringRes override val labelRes: Int,
    @param:StringRes val explanation: Int? = null,
    @param:StringRes val widgetContentDescription: Int
) :
    WidgetProperty {
    LastRefreshTimeDisplay(R.string.display_last_refresh_time),
    RefreshButton(R.string.refresh_button, R.string.refresh_button_explanation),
    GoToWifiSettingsButton(R.string.open_wifi_settings_button, R.string.go_to_wifi_settings_button_explanation),
    GoToWidgetSettingsButton(R.string.open_widget_settings_button, R.string.go_to_widget_settings_button_explanation)
}
