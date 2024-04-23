package com.bangbangbang.screencapture

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream


class RequestScreenshotActivity : AppCompatActivity() {
    private val SCREENSHOT_REQUEST = 100
    private lateinit var mediaProjectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 启动截屏请求
        screenshot()
    }

    private fun screenshot() {
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onActivityResult(SCREENSHOT_REQUEST, it.resultCode, it.data)
        }.launch(intent)
    }

    private fun sendImage(bitmap: Bitmap) {
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()
            val requestBody = byteArray.toRequestBody("image/png".toMediaTypeOrNull(), 0, byteArray.size)
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "screenshot.png", requestBody)
                .build()
            val request = Request.Builder()
                .url("http://59.110.136.81:5000/b-mark")
                .post(body)
                .build()
            val response = OkHttpClient().newCall(request).execute()

            // 将响应体转换为字符串
            val responseData = response.body?.string()

            // 解析json数据
            val jsonObject = JSONObject(responseData)
            val result = jsonObject.getString("result")
            val message = jsonObject.getString("msg")
            Log.d("debug", result)
            Log.d("debug", message)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREENSHOT_REQUEST) {
            val metrics = resources.displayMetrics
            val imageReader = ImageReader.newInstance(
                metrics.widthPixels,
                metrics.heightPixels,
                PixelFormat.RGBA_8888,
                2
            )

            data?.let { mediaProjectionManager.getMediaProjection(resultCode, it) }
                ?.createVirtualDisplay(
                    "screencap",
                    metrics.widthPixels,
                    metrics.heightPixels,
                    metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    imageReader.surface,
                    null,
                    null
                )


            Handler(Looper.getMainLooper()).postDelayed({
                val image = imageReader.acquireLatestImage()
                val bitmap = image?.let {
                    Toast.makeText(this, "正在处理", Toast.LENGTH_SHORT).show()
                    // image转bitmap
                    val plane = it.planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * metrics.widthPixels
                    Bitmap.createBitmap(
                        metrics.widthPixels + rowPadding / pixelStride,
                        metrics.heightPixels,
                        Bitmap.Config.ARGB_8888
                    ).apply {
                        Toast.makeText(this@RequestScreenshotActivity, "截图成功", Toast.LENGTH_SHORT).show()
                        // 发送截图到服务器
                        copyPixelsFromBuffer(buffer)
                        Thread {
                            sendImage(this)

                            runOnUiThread {
                            }
                        }.start()
                    }
                }
                image?.close()
                bitmap?.recycle()
                // 在此之前要确保 imageReader 停止并释放资源
                imageReader.close()
                // 关闭activity
                finish()
            }, 1000) // Delay 1s to acquire image
        }
    }
}
