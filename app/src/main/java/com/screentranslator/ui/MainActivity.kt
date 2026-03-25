package com.screentranslator.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.screentranslator.R
import com.screentranslator.databinding.ActivityMainBinding
import com.screentranslator.service.ScreenTranslationService
import com.screentranslator.utils.LanguageConfig
import com.screentranslator.utils.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefManager: PreferenceManager
    private var isServiceRunning = false

    // Language lists
    private val sourceLanguages = LanguageConfig.SOURCE_LANGUAGES
    private val targetLanguages = LanguageConfig.TARGET_LANGUAGES

    // MediaProjection launcher
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            startTranslationService(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    // Overlay permission launcher
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            requestMediaProjection()
        } else {
            Toast.makeText(this, getString(R.string.overlay_permission_required), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)
        setupUI()
        checkServiceState()
    }

    override fun onResume() {
        super.onResume()
        checkServiceState()
        updateUIState()
    }

    private fun setupUI() {
        // Setup source language spinner
        val sourceAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            sourceLanguages.map { it.displayName }
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerSourceLang.adapter = sourceAdapter

        // Setup target language spinner
        val targetAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            targetLanguages.map { it.displayName }
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerTargetLang.adapter = targetAdapter

        // Restore saved selections
        val savedSourceIdx = prefManager.getSourceLanguageIndex()
        val savedTargetIdx = prefManager.getTargetLanguageIndex()
        binding.spinnerSourceLang.setSelection(savedSourceIdx)
        binding.spinnerTargetLang.setSelection(savedTargetIdx)

        // Source language selection
        binding.spinnerSourceLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                prefManager.saveSourceLanguageIndex(pos)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Target language selection
        binding.spinnerTargetLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                prefManager.saveTargetLanguageIndex(pos)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Capture interval slider
        binding.sliderInterval.value = prefManager.getCaptureInterval().toFloat()
        binding.tvIntervalValue.text = getString(R.string.seconds_format, prefManager.getCaptureInterval())
        binding.sliderInterval.addOnChangeListener { _, value, _ ->
            val secs = value.toInt()
            prefManager.saveCaptureInterval(secs)
            binding.tvIntervalValue.text = getString(R.string.seconds_format, secs)
        }

        // Transparency slider
        binding.sliderTransparency.value = prefManager.getOverlayAlpha()
        binding.tvTransparencyValue.text = "${(prefManager.getOverlayAlpha() * 100).toInt()}%"
        binding.sliderTransparency.addOnChangeListener { _, value, _ ->
            prefManager.saveOverlayAlpha(value)
            binding.tvTransparencyValue.text = "${(value * 100).toInt()}%"
        }

        // Font size selector
        binding.btnFontSmall.setOnClickListener { setFontSize(12) }
        binding.btnFontMedium.setOnClickListener { setFontSize(16) }
        binding.btnFontLarge.setOnClickListener { setFontSize(20) }

        // Start/Stop button
        binding.btnStartStop.setOnClickListener {
            if (isServiceRunning) {
                stopTranslationService()
            } else {
                checkPermissionsAndStart()
            }
        }

        // Info button
        binding.btnInfo.setOnClickListener {
            showInfoDialog()
        }
    }

    private fun setFontSize(size: Int) {
        prefManager.saveFontSize(size)
        val buttons = listOf(binding.btnFontSmall, binding.btnFontMedium, binding.btnFontLarge)
        val sizes = listOf(12, 16, 20)
        buttons.zip(sizes).forEach { (btn, s) ->
            btn.isSelected = s == size
        }
        // Notify service to update
        if (isServiceRunning) {
            val intent = Intent(ScreenTranslationService.ACTION_UPDATE_SETTINGS)
            sendBroadcast(intent)
        }
    }

    private fun checkPermissionsAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog()
        } else {
            requestMediaProjection()
        }
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.overlay_permission_title))
            .setMessage(getString(R.string.overlay_permission_message))
            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun requestMediaProjection() {
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mgr.createScreenCaptureIntent())
    }

    private fun startTranslationService(resultCode: Int, data: Intent) {
        val sourceCode = sourceLanguages[binding.spinnerSourceLang.selectedItemPosition].code
        val targetCode = targetLanguages[binding.spinnerTargetLang.selectedItemPosition].code

        val intent = Intent(this, ScreenTranslationService::class.java).apply {
            action = ScreenTranslationService.ACTION_START
            putExtra(ScreenTranslationService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenTranslationService.EXTRA_DATA, data)
            putExtra(ScreenTranslationService.EXTRA_SOURCE_LANG, sourceCode)
            putExtra(ScreenTranslationService.EXTRA_TARGET_LANG, targetCode)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        isServiceRunning = true
        updateUIState()
        Toast.makeText(this, getString(R.string.service_started), Toast.LENGTH_SHORT).show()
    }

    private fun stopTranslationService() {
        val intent = Intent(this, ScreenTranslationService::class.java).apply {
            action = ScreenTranslationService.ACTION_STOP
        }
        startService(intent)
        isServiceRunning = false
        updateUIState()
        Toast.makeText(this, getString(R.string.service_stopped), Toast.LENGTH_SHORT).show()
    }

    private fun checkServiceState() {
        isServiceRunning = ScreenTranslationService.isRunning
    }

    private fun updateUIState() {
        if (isServiceRunning) {
            binding.btnStartStop.text = getString(R.string.stop_translation)
            binding.btnStartStop.setBackgroundColor(getColor(R.color.stop_color))
            binding.tvStatus.text = getString(R.string.status_running)
            binding.tvStatus.setTextColor(getColor(R.color.status_running))
            binding.ivStatusDot.setColorFilter(getColor(R.color.status_running))
            binding.cardSettings.alpha = 0.6f
            binding.cardSettings.isEnabled = false
        } else {
            binding.btnStartStop.text = getString(R.string.start_translation)
            binding.btnStartStop.setBackgroundColor(getColor(R.color.start_color))
            binding.tvStatus.text = getString(R.string.status_stopped)
            binding.tvStatus.setTextColor(getColor(R.color.status_stopped))
            binding.ivStatusDot.setColorFilter(getColor(R.color.status_stopped))
            binding.cardSettings.alpha = 1.0f
            binding.cardSettings.isEnabled = true
        }
    }

    private fun showInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.how_to_use_title))
            .setMessage(getString(R.string.how_to_use_message))
            .setPositiveButton(getString(R.string.got_it), null)
            .show()
    }
}
