package com.catman.mkl

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.catman.mkl.data.Datasource
import com.catman.mkl.model.TargetImage
import com.catman.mkl.ui.theme.MklTheme
import java.io.File
import java.io.FileNotFoundException
import java.lang.Integer.max
import java.util.UUID


val DEFAULT_IMG = R.drawable.scotland_house_upscale
val MAX_DIM = 1920


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MklTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MklApp()
                }
            }
        }
    }
}


@Composable
fun MklApp() {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val defaultImage = BitmapFactory.decodeResource(context.resources, DEFAULT_IMG)

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(defaultImage) }
    var activeImageId by remember { mutableStateOf<Int?>(null) }
    var cachedFilter = remember { ColorMatrixColorFilter(ColorMatrix()) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
        {uri ->
            Log.d("MklApp", uri.toString())
            sourceBitmap = getSourceBitmap(context, uri)
        }
    )

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
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledTonalButton(
                onClick = {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            ) {
                Text("Select Image")
            }
            SourceImage(sourceBitmap, cachedFilter)
            Text("You've selected  $activeImageId")
            ImageList(
                activeImageId = activeImageId,
                onClick = {imageResourceId ->
                    if (activeImageId == imageResourceId) {
                        // deselect
                        activeImageId = null
                        cachedFilter = ColorMatrixColorFilter(ColorMatrix())
                    } else {
                        // select
                        activeImageId = imageResourceId
                        if (imageResourceId != null) {
                            cachedFilter = onSelectBitmap(context, sourceBitmap, imageResourceId)
                        }
                    }
                },
                imageList = Datasource().loadTargetImages()
            )
        }
    }
}


fun getSourceBitmap(context: Context, sourceImageUri: Uri?): Bitmap? {
    var user_bitmap: Bitmap? = null
    try {
        if (sourceImageUri != null) {
            context.contentResolver.openInputStream(sourceImageUri)?.use {
                    input ->
                user_bitmap = BitmapFactory.decodeStream(input)
                user_bitmap = rescaleBitmap(user_bitmap)
            }
        }
    } catch (
        e: FileNotFoundException
    ) {
        Log.e("MklApp", "Could not load an image from contentResolver")
        e.printStackTrace()
        return null
    }
    return user_bitmap
}

fun rescaleBitmap(bitmap: Bitmap?, allowed_dim: Int = MAX_DIM): Bitmap? {
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

fun onSelectBitmap(context: Context, sourceBitmap: Bitmap?, activeImageId: Int): ColorMatrixColorFilter {
    val start = System.nanoTime()

    val bitmap_0 = sourceBitmap ?: BitmapFactory.decodeResource(context.resources, DEFAULT_IMG)

    val bitmap_1 = BitmapFactory.decodeResource(
        context.resources, activeImageId
    )
    val res0 = getMeanCov(bitmap_0)
    val mean0 = res0[0]
    val cov0 = res0[1]

    val res1 = getMeanCov(bitmap_1)
    val mean1 = res1[0]
    val cov1 = res1[1]

    val F = MKL(cov0, cov1, mean0, mean1)
    val colorFilter = ColorMatrixColorFilter(ColorMatrix(F))

    Log.d("onSelectBitmap, ms", ((System.nanoTime() - start)/1000000).toString())
    return colorFilter
}


fun applyFilter(bitmap: Bitmap, filter: ColorFilter?) : Bitmap {
    var result = Bitmap.createBitmap(
        bitmap.getWidth(),
        bitmap.getHeight(), Bitmap.Config.ARGB_8888
    )
    val canvas: Canvas = Canvas(result)
    val paint: Paint = Paint()
    paint.colorFilter = filter?.asAndroidColorFilter()
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return result
}


@Composable
fun SourceImage(sourceBitmap: Bitmap?, cachedFilter: ColorMatrixColorFilter) {
    val context = LocalContext.current
    val bitmap = sourceBitmap ?: BitmapFactory.decodeResource(context.resources, DEFAULT_IMG)

    var buttonCount by remember { mutableStateOf(0)}
    var result = applyFilter(bitmap, cachedFilter)

    Image(
        result.asImageBitmap(),
        contentDescription = "user picture",
        modifier = Modifier
            .width(512.dp)
            .height(512.dp),
        contentScale = ContentScale.Inside,
    )

    FilledTonalButton(
        onClick = {
            buttonCount += 1
            val isSaved = saveImageToMediaStore(
                context = context,
                bitmap = result,
                displayName = "user_image_${buttonCount}_${UUID.randomUUID()}",
                mimeType = "image/jpeg" // or "image/png"
            )
            if (isSaved != null) {
                Log.d("MediaStore", "Image saved successfully at: $isSaved")
            } else {
                Log.e("MediaStore", "Failed to save image.")
            }
        }
    ) {
        Text("Save Image $buttonCount")
    }
}

/**
 * Saves a Bitmap image to the MediaStore, making it accessible by other apps.
 * This function is relevant for Android 9 (API level 28) and higher.
 *
 * @param context The application context.
 * @param bitmap The Bitmap image to be saved.
 * @param displayName The desired name of the image file (without extension).
 * @param mimeType The MIME type of the image (e.g., "image/jpeg", "image/png").
 * @param directoryName (Optional) The subdirectory within the standard media folders (e.g., Environment.DIRECTORY_PICTURES).
 *                     If null, the image will be saved directly in the respective standard folder.
 * @return The Uri of the saved image if successful, otherwise null.
 */
fun saveImageToMediaStore(
    context: Context,
    bitmap: Bitmap,
    displayName: String,
    mimeType: String,
//    directoryName: String? = Environment.DIRECTORY_PICTURES
): Uri? {
    val contentResolver = context.contentResolver
    val imageName = "$displayName.${mimeType.substringAfter("/")}" // Add extension based on MIME type

    val imagePath = Environment.DIRECTORY_PICTURES + File.separator + "MKL"

    // ContentValues specify the metadata of the media file you want to save.
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, imageName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

        // For Android 10 (API level 29) and above, specify the relative path within the standard folders.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, imagePath ?: Environment.DIRECTORY_PICTURES)
            put(MediaStore.MediaColumns.IS_PENDING, 1) // Mark as pending until data is written
        }
//        } else {
//            // For Android 9 (API level 28), use the absolute path within the external storage.
//            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "")
//            if (!directory.exists()) {
//                directory.mkdirs()
//            }
//            put(MediaStore.MediaColumns.DATA, File(directory, imageName).absolutePath)
//        }
    }
    var uri: Uri? = null
    try {
        // Insert the metadata into the MediaStore to get a content URI.
        uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let { imageUri ->
            // Open an OutputStream to write the image data to the MediaStore.
            contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                // Compress the Bitmap and write it to the OutputStream.
                val compressFormat =
                    if (mimeType == "image/png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                bitmap.compress(compressFormat, 100, outputStream)

                // For Android 10 and above, clear the IS_PENDING flag to make the file publicly available.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(imageUri, contentValues, null, null)
                }
                return imageUri
            }
        }
    } catch (e: Exception) {
        // If an error occurs, delete the pending file.
        uri?.let { contentResolver.delete(it, null, null) }
        e.printStackTrace() // Consider logging the error more robustly.
    }
    return null
}


@Composable
fun ImageList(
    activeImageId: Int?,
    onClick: (Int?) -> Unit,
    imageList: List<TargetImage>,
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
    }
}

@Composable
fun ImageCard(image: TargetImage, selected: Boolean, modifier: Modifier = Modifier) {
    var border: BorderStroke? = null
    if (selected) {
        border = BorderStroke(5.dp, Color.White)
    }
    Card(modifier = modifier, border = border) {
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


@Preview
@Composable
private fun ImageCardPreview() {
    ImageCard(TargetImage(R.drawable.pastel_small, R.string.pastel), false)
}
