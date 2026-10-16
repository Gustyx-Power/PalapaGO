package id.gustyxganteng.agent.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressor {

    suspend fun compressUriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1280,
        quality: Int = 80
    ): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")

        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val width = originalBitmap.width
        val height = originalBitmap.height
        val maxSide = max(width, height)

        val scaledBitmap = if (maxSide > maxDimension) {
            val scale = maxDimension.toFloat() / maxSide
            Bitmap.createScaledBitmap(
                originalBitmap,
                (width * scale).toInt(),
                (height * scale).toInt(),
                true
            )
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()

        Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
