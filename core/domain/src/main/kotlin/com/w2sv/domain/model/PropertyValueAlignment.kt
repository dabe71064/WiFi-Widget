package com.w2sv.domain.model

import androidx.annotation.StringRes
import com.w2sv.core.common.R

enum class PropertyValueAlignment(@param:StringRes override val labelRes: Int) : WidgetProperty {
    Left(R.string.left),
    Right(R.string.right)
}
