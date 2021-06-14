package com.nikhiljain.autospeech

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.nikhiljain.autospeech.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var languageIdentifier: LanguageIdentifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initLanguageIdentifier()

        binding.buttonDetectLanguage.setOnClickListener {
            detectLanguage()
        }
    }

    private fun initLanguageIdentifier() {
        languageIdentifier = LanguageIdentification.getClient()
    }

    private fun detectLanguage() {
        binding.textViewDetectedLanguage.text = null
        val text = binding.editTextLanguageText.text.toString()
            .takeIf { it.isNotEmpty() } ?: run {
            showToastMessage(R.string.error_message_empty_text)
            return
        }

        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    showToastMessage(R.string.error_message_undefined_language_detected)
                } else {
                    binding.textViewDetectedLanguage.text =
                        getString(R.string.text_detected_language, Locale(languageCode).displayName)
                }
            }
            .addOnFailureListener {
                showToastMessage(R.string.error_message_something_went_wrong)
            }
    }

    private fun showToastMessage(@StringRes resId : Int ) {
        Toast.makeText(
            this,
            getString(resId),
            Toast.LENGTH_SHORT
        ).show()
    }
}