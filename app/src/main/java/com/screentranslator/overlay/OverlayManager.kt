package com.screentranslator.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.util.TypedValue
import android.view.*
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.screentranslator.R
import com.screentranslator.utils.PreferenceManager

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefManager = PreferenceManager(context)
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var tvOriginalText: TextView? = null
    private var tvTranslatedText: TextView? = null
    private var btnClose: ImageButton? = null
    private var btnMinimize: ImageButton? = null
    private var scrollView: ScrollView? = null
    private var headerView: View? = null
    private var contentCard: CardView? = null

    private var isMinimized = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    fun show() {
        if (overlayView != null) return

        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(R.layout.overlay_translation, null)

        tvOriginalText = overlayView?.findViewById(R.id.tv_original_text)
        tvTranslatedText = overlayView?.findViewById(R.id.tv_translated_text)
        btnClose = overlayView?.findViewById(R.id.btn_close)
        btnMinimize = overlayView?.findViewById(R.id.btn_minimize)
        scrollView = overlayView?.findViewById(R.id.scroll_view)
        headerView = overlayView?.findViewById(R.id.overlay_header)
        contentCard = overlayView?.findViewById(R.id.content_card)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 200
        }

        applySettings()
        setupDrag()
        setupButtons()

        windowManager.addView(overlayView, layoutParams)
    }

    fun hide() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }
    }

    fun updateTranslation(original: String, translated: String) {
        if (overlayView == null) return

        tvOriginalText?.text = original
        tvTranslatedText?.text = translated

        // Auto-show if minimized when new content arrives
        if (isMinimized) {
            isMinimized = false
            scrollView?.visibility = View.VISIBLE
            btnMinimize?.setImageResource(R.drawable.ic_minimize)
        }
    }

    fun updateSettings() {
        applySettings()
        layoutParams?.let { windowManager.updateViewLayout(overlayView, it) }
    }

    private fun applySettings() {
        val alpha = prefManager.getOverlayAlpha()
        val fontSize = prefManager.getFontSize()

        overlayView?.alpha = alpha
        tvOriginalText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize.toFloat())
        tvTranslatedText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, (fontSize + 2).toFloat())
    }

    private fun setupDrag() {
        headerView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                    overlayView?.let { windowManager.updateViewLayout(it, layoutParams) }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupButtons() {
        btnClose?.setOnClickListener {
            // Stop service from overlay
            val intent = android.content.Intent(context, com.screentranslator.service.ScreenTranslationService::class.java).apply {
                action = com.screentranslator.service.ScreenTranslationService.ACTION_STOP
            }
            context.startService(intent)
            hide()
        }

        btnMinimize?.setOnClickListener {
            isMinimized = !isMinimized
            if (isMinimized) {
                scrollView?.visibility = View.GONE
                btnMinimize?.setImageResource(R.drawable.ic_expand)
            } else {
                scrollView?.visibility = View.VISIBLE
                btnMinimize?.setImageResource(R.drawable.ic_minimize)
            }
            overlayView?.let { windowManager.updateViewLayout(it, layoutParams) }
        }
    }
}
