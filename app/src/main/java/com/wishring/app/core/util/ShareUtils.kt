package com.wishring.app.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SNS 공유를 위한 유틸리티 클래스
 */
object ShareUtils {
    
    private const val SHARE_FOLDER = "share_images"
    private const val IMAGE_QUALITY = 100
    
    /**
     * Composable을 Bitmap으로 캡처
     */
    suspend fun captureComposable(
        context: Context,
        content: @Composable () -> Unit,
        width: Int = 1080,
        height: Int = 1920
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val composeView = ComposeView(context).apply {
                setContent { content() }
            }
            
            // View를 측정하고 레이아웃
            val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
            
            composeView.measure(widthSpec, heightSpec)
            composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)
            
            // Bitmap 생성
            val bitmap = Bitmap.createBitmap(
                composeView.measuredWidth,
                composeView.measuredHeight,
                Bitmap.Config.ARGB_8888
            )
            
            val canvas = Canvas(bitmap)
            composeView.draw(canvas)
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Bitmap을 파일로 저장
     */
    suspend fun saveBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        fileName: String? = null
    ): File? = withContext(Dispatchers.IO) {
        try {
            val shareDir = File(context.cacheDir, SHARE_FOLDER)
            if (!shareDir.exists()) {
                shareDir.mkdirs()
            }
            
            val finalFileName = fileName ?: generateImageFileName()
            val file = File(shareDir, finalFileName)
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, IMAGE_QUALITY, out)
            }
            
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 이미지와 텍스트를 함께 공유
     */
    fun shareImageWithText(
        context: Context,
        imageFile: File,
        message: String,
        hashtags: String = ""
    ) {
        try {
            val imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            
            val fullMessage = buildString {
                append(message)
                if (hashtags.isNotEmpty()) {
                    append("\n\n")
                    // 해시태그가 #으로 시작하지 않으면 자동으로 추가
                    val formattedHashtags = hashtags
                        .split(" ")
                        .filter { it.isNotEmpty() }
                        .joinToString(" ") { tag ->
                            if (tag.startsWith("#")) tag else "#$tag"
                        }
                    append(formattedHashtags)
                }
            }
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, fullMessage)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(shareIntent, "WISH RING 공유하기")
            context.startActivity(chooser)
            
        } catch (e: Exception) {
            e.printStackTrace()
            // 이미지 공유가 실패하면 텍스트만 공유
            shareTextOnly(context, message, hashtags)
        }
    }
    
    /**
     * 텍스트만 공유 (이미지 실패 시 대안)
     */
    private fun shareTextOnly(
        context: Context,
        message: String,
        hashtags: String = ""
    ) {
        try {
            val fullMessage = buildString {
                append(message)
                if (hashtags.isNotEmpty()) {
                    append("\n\n")
                    val formattedHashtags = hashtags
                        .split(" ")
                        .filter { it.isNotEmpty() }
                        .joinToString(" ") { tag ->
                            if (tag.startsWith("#")) tag else "#$tag"
                        }
                    append(formattedHashtags)
                }
            }
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, fullMessage)
            }
            
            val chooser = Intent.createChooser(shareIntent, "WISH RING 공유하기")
            context.startActivity(chooser)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 공유용 이미지 파일명 생성
     */
    private fun generateImageFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "wishring_share_$timestamp.png"
    }
    
    /**
     * 공유 폴더 정리 (오래된 파일 삭제)
     */
    fun cleanOldShareFiles(context: Context, maxAgeMs: Long = 24 * 60 * 60 * 1000L) {
        try {
            val shareDir = File(context.cacheDir, SHARE_FOLDER)
            if (!shareDir.exists()) return
            
            val currentTime = System.currentTimeMillis()
            shareDir.listFiles()?.forEach { file ->
                if (currentTime - file.lastModified() > maxAgeMs) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * 공유할 콘텐츠 데이터 클래스
 */
data class ShareContent(
    val message: String,
    val hashtags: String = "",
    val imageFile: File? = null
)