package com.catman.mkl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.catman.mkl.data.Datasource
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.lang.Integer.max


val DEFAULT_IMG = R.drawable.scotland_house_upscale
const val MAX_SOURCE_DIM = 1920
const val MAX_TARGET_DIM = 512


@Composable
fun MklApp() {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    val defaultImage = BitmapFactory.decodeResource(context.resources, DEFAULT_IMG)
    var sourceBitmap by remember { mutableStateOf<Bitmap>(defaultImage) }
    var sourceWidth = screenWidth
    var sourceHeight = sourceBitmap.height.dp * (screenWidth / sourceBitmap.width.dp)
    sourceHeight = minOf(screenHeight / 2, sourceHeight)

    var userTargetBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var activeImageId by remember { mutableStateOf<Int?>(null) }
    var cachedMatrix = remember { ColorMatrix() }
    var userImageList by remember { mutableStateOf<List<Bitmap?>>(mutableListOf()) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
        { uri ->
            Log.d("MklApp", uri.toString())
            sourceBitmap = getSourceBitmap(context, uri)
        }
    )

    val pickUserTarget = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
        { uri ->
            Log.d("MklApp", uri.toString())
            userTargetBitmap = getTargetBitmap(context, uri)
            if (userTargetBitmap != null) {
                userImageList += userTargetBitmap
            }
        }
    )

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { contentPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(
                    start = WindowInsets.safeDrawing
                        .asPaddingValues()
                        .calculateStartPadding(layoutDirection),
                    end = WindowInsets.safeDrawing
                        .asPaddingValues()
                        .calculateEndPadding(layoutDirection),
                ),
            color = MaterialTheme.colorScheme.surfaceBright
        ) {
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                FilledTonalButton(
                    onClick = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(5),
                ) {
                    Text("Select image to edit")
                }
                SourceImage(
                    sourceBitmap,
                    cachedMatrix,
                    modifier = Modifier
                        .width(sourceWidth)
                        .height(sourceHeight),
                    onSave = { message ->
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                )
                ImageList(
                    activeImageId = activeImageId,
                    onClick = { imageId ->
                        if (activeImageId == imageId) {
                            // Deselect
                            activeImageId = null
                            cachedMatrix = ColorMatrix()
                        } else {
                            // Select
                            activeImageId = imageId
                            if (imageId != null) {
                                var activeBitmap: Bitmap? = null
                                if (imageId < userImageList.size) {
                                    activeBitmap = userImageList[imageId]
                                }
                                cachedMatrix =
                                    onSelectBitmap(context, sourceBitmap, imageId, activeBitmap)
                            }
                        }
                    },
                    imageList = Datasource().loadTargetImages(),
                    userImageList = userImageList,
                    onButtonClick = {
                        pickUserTarget.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
            }
        }
    }
}


fun getSourceBitmap(context: Context, sourceImageUri: Uri?): Bitmap {
    var user_bitmap: Bitmap? = null
    try {
        if (sourceImageUri != null) {
            context.contentResolver.openInputStream(sourceImageUri)?.use { input ->
                user_bitmap = BitmapFactory.decodeStream(input)
                user_bitmap = rescaleBitmap(user_bitmap, MAX_SOURCE_DIM)
            }
        }
        if (user_bitmap != null) {
            return user_bitmap as Bitmap
        } else {
            return BitmapFactory.decodeResource(context.resources, DEFAULT_IMG)
        }
    } catch (
        e: FileNotFoundException
    ) {
        Log.e("MklApp", "Could not load an image from contentResolver")
        e.printStackTrace()
        return BitmapFactory.decodeResource(context.resources, DEFAULT_IMG) //null
    }
}


fun getTargetBitmap(context: Context, sourceImageUri: Uri?): Bitmap? {
    var user_bitmap: Bitmap? = null
    try {
        if (sourceImageUri != null) {
            context.contentResolver.openInputStream(sourceImageUri)?.use { input ->
                user_bitmap = BitmapFactory.decodeStream(input)
                user_bitmap = rescaleBitmap(user_bitmap, MAX_TARGET_DIM)
            }
        }
        return user_bitmap
    } catch (
        e: FileNotFoundException
    ) {
        Log.e("MklApp", "Could not load an image from contentResolver")
        e.printStackTrace()
        return null
    }
}


fun rescaleBitmap(bitmap: Bitmap?, allowed_dim: Int = MAX_TARGET_DIM): Bitmap? {
    if (bitmap != null) {
        val max_dim = max(bitmap.width, bitmap.height)
        if (max_dim > allowed_dim) {
            val new_width = bitmap.width * allowed_dim / max_dim
            val new_height = bitmap.height * allowed_dim / max_dim
            return Bitmap.createScaledBitmap(bitmap, new_width, new_height, true)
        }
    }
    return bitmap
}


fun onSelectBitmap(
    context: Context,
    sourceBitmap: Bitmap?,
    activeImageId: Int,
    activeBitmap: Bitmap?,
): ColorMatrix {
    val start = System.nanoTime()

    val bitmap_0 = sourceBitmap ?: BitmapFactory.decodeResource(context.resources, DEFAULT_IMG)
    val bitmap_1 = activeBitmap ?: BitmapFactory.decodeResource(
        context.resources, activeImageId
    )
    val res0 = getMeanCov(bitmap_0)
    val mean0 = res0[0]
    val cov0 = res0[1]

    val res1 = getMeanCov(bitmap_1)
    val mean1 = res1[0]
    val cov1 = res1[1]

    val F = MKL(cov0, cov1, mean0, mean1)
    val colorMatrix = ColorMatrix(F)

    Log.d("onSelectBitmap, ms", ((System.nanoTime() - start) / 1000000).toString())
    return colorMatrix
}