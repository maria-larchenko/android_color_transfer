package com.catman.mkl.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.colorspace.Rgb

data class TargetImage (
     @DrawableRes val imageResourceId: Int,
     @StringRes val stringResourceId: Int,
)