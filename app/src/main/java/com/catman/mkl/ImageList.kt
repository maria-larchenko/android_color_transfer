package com.catman.mkl

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.catman.mkl.model.TargetImage
import com.catman.mkl.model.UserTargetImage


@Composable
fun ImageList(
    activeImageId: Int?,
    onClick: (Int?) -> Unit,
    onButtonClick: () -> Unit,
    imageList: List<TargetImage>,
    userImageList: List<Bitmap?>,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier) {
        items(imageList) { image ->
            ImageCard(
                image = image,
                selected = image.imageResourceId == activeImageId,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable(onClick = { onClick(image.imageResourceId) })
            )
        }
        itemsIndexed(userImageList) { index, bitmap ->
            if (bitmap != null) {
                val image = UserTargetImage(bitmap, index, R.string.user_target_image)
                UserImageCard(
                    image = image,
                    selected = image.imageResourceId == activeImageId,
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable(onClick = { onClick(image.imageResourceId) })
                )
            }
        }
        item {
            OutlinedButton(
                onClick = onButtonClick,
                shape = RoundedCornerShape(5), // 50% percent = CircleShape
                modifier = Modifier
                    .padding(8.dp)
                    .height(256.dp)
                    .width(256.dp)
            ) {
                Text("Add your style image")
            }
        }
    }
}


@Composable
fun ImageCard(image: TargetImage, selected: Boolean, modifier: Modifier = Modifier) {
    var border: BorderStroke? = null
    if (selected) {
        border = BorderStroke(5.dp, MaterialTheme.colorScheme.onSurface)
    }
    Card(
        modifier = modifier,
        border = border,
    ) {
        Image(
            painter = painterResource(image.imageResourceId),
            contentDescription = stringResource(image.stringResourceId),
            modifier = Modifier
                .width(256.dp)
                .height(256.dp),
            contentScale = ContentScale.FillHeight
        )
    }
}


@Composable
fun UserImageCard(image: UserTargetImage, selected: Boolean, modifier: Modifier = Modifier) {
    var border: BorderStroke? = null
    if (selected) {
        border = BorderStroke(5.dp, MaterialTheme.colorScheme.onSurface)
    }
    Card(modifier = modifier, border = border) {
        Image(
            image.bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .width(256.dp)
                .height(256.dp),
            contentScale = ContentScale.FillHeight
        )
    }
}


@Preview
@Composable
private fun ImageCardPreview() {
    ImageCard(TargetImage(R.drawable.pastel_small, R.string.pastel), true)
}

