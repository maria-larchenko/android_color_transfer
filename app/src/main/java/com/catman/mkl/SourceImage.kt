package com.catman.mkl

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File


@Composable
fun SourceImage(
    sourceBitmap: Bitmap?,
    cachedMatrix: ColorMatrix,
    modifier: Modifier,
    onSave: (String) -> Unit,
) {
    val context = LocalContext.current
    val bitmap = sourceBitmap ?: BitmapFactory.decodeResource(context.resources, DEFAULT_IMG)

    var sliderPosition by remember { mutableFloatStateOf(1.0f) }
    var result = applyFilter(bitmap, cachedMatrix, sliderPosition)

    Image(
        result.asImageBitmap(),
        contentDescription = "user picture",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
    Slider(
        value = sliderPosition,
        onValueChange = { sliderPosition = it },
        modifier = Modifier.padding(horizontal = 40.dp)
    )
    FilledTonalButton(
        onClick = {
            val imagePath = Environment.DIRECTORY_PICTURES + File.separator + "MKL"
            val fileName = "mkl_${System.currentTimeMillis()}"
            val isSaved = saveImageToMediaStore(
                context = context,
                bitmap = result,
                displayName = fileName,
                imagePath = imagePath,
                mimeType = "image/jpeg" // or "image/png"
            )
            if (isSaved != null) {
                Log.d("MediaStore", "Image saved successfully at: $isSaved")
            } else {
                Log.e("MediaStore", "Failed to save image.")
            }
            onSave("Saved $fileName at  $imagePath")
        },
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(5),
    ) {
        Text("Save result")
    }
}


fun applyFilter(bitmap: Bitmap, cachedMatrix: ColorMatrix?, slider: Float) : Bitmap {
    var result = Bitmap.createBitmap(
        bitmap.getWidth(),
        bitmap.getHeight(), Bitmap.Config.ARGB_8888
    )
    val canvas: Canvas = Canvas(result)
    val paint: Paint = Paint()

    val identity = ColorMatrix()
    val matrix = ColorMatrix()
    if (cachedMatrix != null) {
        for (i in 0..19) {
            matrix.values[i] = slider * cachedMatrix.values[i] + (1 - slider) * identity.values[i]
        }
    }
    paint.colorFilter = ColorMatrixColorFilter(matrix).asAndroidColorFilter()
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return result
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
    imagePath: String = Environment.DIRECTORY_PICTURES + File.separator + "MKL",
): Uri? {
    val contentResolver = context.contentResolver
    val imageName = "$displayName.${mimeType.substringAfter("/")}" // Add extension based on MIME type

    // ContentValues specify the metadata of the media file
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, imageName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

        // For Android 10 (API level 29) and above, specify the relative path within the standard folders.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, imagePath)
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
        // Insert the metadata into the MediaStore to get a content URI
        uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let { imageUri ->
            contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                val compressFormat =
                    if (mimeType == "image/png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                bitmap.compress(compressFormat, 100, outputStream)

                // For Android 10 and above, clear the IS_PENDING flag to make the file publicly available
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(imageUri, contentValues, null, null)
                }
                return imageUri
            }
        }
    } catch (e: Exception) {
        // If an error occurs, delete the pending file
        uri?.let { contentResolver.delete(it, null, null) }
        e.printStackTrace()
    }
    return null
}
