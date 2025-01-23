package com.catman.mkl.data

import com.catman.mkl.R
import com.catman.mkl.model.TargetImage

class Datasource {
    fun loadTargetImages(): List<TargetImage> {
        return listOf<TargetImage>(
            TargetImage(R.drawable.pastel_small, R.string.pastel),
            TargetImage(R.drawable.scotland_plain_upscale, R.string.scotland_plain),
            TargetImage(R.drawable.dncm_sky, R.string.dncm_sky),
            TargetImage(R.drawable.dncm_girl, R.string.dncm_girl)
        )
    }
}