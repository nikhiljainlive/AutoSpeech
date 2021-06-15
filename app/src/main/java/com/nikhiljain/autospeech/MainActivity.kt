package com.nikhiljain.autospeech

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.nikhiljain.autospeech.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var languageIdentifier: LanguageIdentifier
    private lateinit var textToSpeech: TextToSpeech
    private var languageCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initLanguageIdentifier()
        initTextToSpeech()
        addPitchSliderValueChangeListener()
        addSpeechRateSliderValueChangeListener()
        setClickOnDetectLangButton()
        setClickOnReadTextButton()
    }

    private fun setClickOnReadTextButton() {
        binding.buttonReadText.setOnClickListener {
            convertTextToSpeech()
        }
    }

    private fun setClickOnDetectLangButton() {
        binding.buttonDetectLanguage.setOnClickListener {
            detectLanguage()
        }
    }

    private fun addPitchSliderValueChangeListener() {
        val defaultPitch = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.TTS_DEFAULT_PITCH, 100
        )
        binding.sliderPitch.value = defaultPitch.toFloat() / 100
        binding.sliderPitch.addOnChangeListener { _, value, _ ->
            if (::textToSpeech.isInitialized) textToSpeech.setPitch(value)
        }
    }

    private fun addSpeechRateSliderValueChangeListener() {
        val defaultSpeechRate = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.TTS_DEFAULT_RATE, 100
        )
        binding.sliderSpeechRate.value = defaultSpeechRate.toFloat() / 100
        binding.sliderSpeechRate.addOnChangeListener { _, value, _ ->
            if (::textToSpeech.isInitialized) textToSpeech.setSpeechRate(value)
        }
    }

    private fun initLanguageIdentifier() {
        languageIdentifier = LanguageIdentification.getClient()
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(this) {
            when (it) {
                TextToSpeech.SUCCESS -> {
                    binding.buttonReadText.isEnabled = true

                    Log.e(TAG, "initTextToSpeech: engine ${textToSpeech.Engine()}")
                    textToSpeech.engines.forEach enginesInfo@{ engineInfo ->
                        if (engineInfo.name.contains("google", ignoreCase = true)) {
                            setGoogleTTSEngineIfPresent(engineInfo.name)
                            return@enginesInfo
                        }
                    }
                }

                TextToSpeech.ERROR -> {
                    binding.buttonReadText.isEnabled = false
                }
            }
        }
    }

    private fun setGoogleTTSEngineIfPresent(engineName: String) {
        textToSpeech = TextToSpeech(this, {
            when (it) {
                TextToSpeech.ERROR -> {
                    binding.buttonReadText.isEnabled = false
                }
            }
        }, engineName)
    }

    private fun detectLanguage() {
        binding.textViewDetectedLanguage.text = null
        val text = binding.editTextLanguageText.text.toString()
            .takeIf { it.isNotEmpty() } ?: run {
            this.languageCode = null
            showToastMessage(R.string.error_message_empty_text)
            return
        }

        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    this.languageCode = null
                    showToastMessage(R.string.error_message_undefined_language_detected)
                } else {
                    this.languageCode = languageCode
                    binding.textViewDetectedLanguage.text =
                        getString(
                            R.string.text_detected_language,
                            Locale.forLanguageTag(languageCode).displayName
                        )
                }
            }
            .addOnFailureListener {
                showToastMessage(R.string.error_message_something_went_wrong)
            }
    }

    private fun convertTextToSpeech() {
        languageCode?.let {
            val langAvailable = textToSpeech.setLanguage(Locale.forLanguageTag(it))
            if (langAvailable == TextToSpeech.LANG_MISSING_DATA ||
                langAvailable == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                showDownloadLanguageDialog(Locale.forLanguageTag(it).displayName)
                return
            }

            val text = binding.editTextLanguageText.text.toString()
            if (text.isEmpty().not()) {
                textToSpeech.speak(
                    text, TextToSpeech.QUEUE_FLUSH,
                    null, "TEXT"
                )
            }
        }
    }

    private fun showDownloadLanguageDialog(language: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.text_language_not_supported, language))
            .setMessage(getString(R.string.error_message_download_language, language))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    val installIntent = Intent()
                    installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                    startActivity(installIntent)
                } catch (exception: Exception) {
                    exception.printStackTrace()
                    showToastMessage(R.string.error_message_something_went_wrong)
                }
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }.create()
            .show()
    }

    private fun showToastMessage(@StringRes resId: Int) {
        Toast.makeText(
            this,
            getString(resId),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}