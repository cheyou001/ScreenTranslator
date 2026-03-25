package com.screentranslator.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.screentranslator.R
import com.screentranslator.overlay.OverlayManager
import com.screentranslator.utils.PreferenceManager
import kotlinx.coroutines.*

class ScreenTranslationService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_SETTINGS = "ACTION_UPDATE_SETTINGS"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        const val EXTRA_SOURCE_LANG = "EXTRA_SOURCE_LANG"
        const val EXTRA_TARGET_LANG = "EXTRA_TARGET_LANG"

        private const val NOTIFICATION_CHANNEL_ID = "screen_translator_channel"
        private const val NOTIFICATION_ID = 1001
        private const val VIRTUAL_DISPLAY_NAME = "ScreenTranslator"
        private const val TAG = "ScreenTranslationSvc"

        @Volatile
        var isRunning = false
            private set
    }

    private lateinit var prefManager: PreferenceManager
    private lateinit var overlayManager: OverlayManager

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var sourceLanguage: String = TranslateLanguage.ENGLISH
    private var targetLanguage: String = TranslateLanguage.CHINESE

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var captureRunnable: Runnable? = null
    private var isProcessing = false
    private var lastTranslatedText = ""

    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            overlayManager.updateSettings()
        }
    }

    // ML Kit Text Recognizer
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun onCreate() {
        super.onCreate()
        prefManager = PreferenceManager(this)
        overlayManager = OverlayManager(this)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsReceiver, IntentFilter(ACTION_UPDATE_SETTINGS), RECEIVER_EXPORTED)
        } else {
            registerReceiver(settingsReceiver, IntentFilter(ACTION_UPDATE_SETTINGS))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                sourceLanguage = intent.getStringExtra(EXTRA_SOURCE_LANG) ?: TranslateLanguage.ENGLISH
                targetLanguage = intent.getStringExtra(EXTRA_TARGET_LANG) ?: TranslateLanguage.CHINESE

                if (data != null) {
                    startForeground(NOTIFICATION_ID, buildNotification())
                    initMediaProjection(resultCode, data)
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun initMediaProjection(resultCode: Int, data: Intent) {
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mgr.getMediaProjection(resultCode, data)

        val metrics = DisplayMetrics()
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
            metrics.densityDpi = resources.displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getMetrics(metrics)
        }

        // Use half resolution for performance
        val width = metrics.widthPixels / 2
        val height = metrics.heightPixels / 2
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        overlayManager.show()
        isRunning = true
        scheduleCaptureLoop()
    }

    private fun scheduleCaptureLoop() {
        val intervalMs = prefManager.getCaptureInterval() * 1000L

        captureRunnable = object : Runnable {
            override fun run() {
                if (isRunning) {
                    captureAndTranslate()
                    handler.postDelayed(this, intervalMs)
                }
            }
        }
        handler.post(captureRunnable!!)
    }

    private fun captureAndTranslate() {
        if (isProcessing) return
        isProcessing = true

        val image: Image? = imageReader?.acquireLatestImage()
        if (image == null) {
            isProcessing = false
            return
        }

        val bitmap = imageToBitmap(image)
        image.close()

        if (bitmap == null) {
            isProcessing = false
            return
        }

        serviceScope.launch {
            try {
                val text = recognizeText(bitmap)
                bitmap.recycle()

                if (text.isBlank() || text == lastTranslatedText) {
                    isProcessing = false
                    return@launch
                }

                lastTranslatedText = text
                val translated = translateText(text)

                withContext(Dispatchers.Main) {
                    overlayManager.updateTranslation(text, translated)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during capture/translate", e)
            } finally {
                isProcessing = false
            }
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            // Crop to exact size
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height).also {
                if (it != bitmap) bitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to bitmap", e)
            null
        }
    }

    private suspend fun recognizeText(bitmap: Bitmap): String {
        return suspendCancellableCoroutine { cont ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(inputImage)
                .addOnSuccessListener { result ->
                    // Group text blocks and clean up
                    val text = result.textBlocks
                        .joinToString("\n") { block ->
                            block.lines.joinToString(" ") { it.text }
                        }
                        .trim()
                    cont.resume(text) {}
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "OCR failed", e)
                    cont.resume("") {}
                }
        }
    }

    private suspend fun translateText(text: String): String {
        // Truncate very long text
        val truncated = if (text.length > 2000) text.substring(0, 2000) + "..." else text

        return suspendCancellableCoroutine { cont ->
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()

            val translator = Translation.getClient(options)

            // Download model if needed then translate
            translator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    translator.translate(truncated)
                        .addOnSuccessListener { translated ->
                            translator.close()
                            cont.resume(translated) {}
                        }
                        .addOnFailureListener { e ->
                            translator.close()
                            Log.e(TAG, "Translation failed", e)
                            cont.resume(text) {}
                        }
                }
                .addOnFailureListener { e ->
                    translator.close()
                    Log.e(TAG, "Model download failed", e)
                    cont.resume("[模型下载失败，请检查网络]") {}
                }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setSound(null, null)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, ScreenTranslationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = packageManager.getLaunchIntentForPackage(packageName)
        val openPi = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_translate)
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(R.drawable.ic_stop, getString(R.string.stop_translation), stopPi)
            .build()
    }

    private fun cleanup() {
        isRunning = false
        captureRunnable?.let { handler.removeCallbacks(it) }
        captureRunnable = null
        overlayManager.hide()
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
        serviceScope.cancel()
        textRecognizer.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        try {
            unregisterReceiver(settingsReceiver)
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
