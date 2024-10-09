package com.sumeyyesahin.olumlamalar.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.sumeyyesahin.olumlamalar.R
import com.sumeyyesahin.olumlamalar.utils.Constants
import java.util.Locale
import kotlin.random.Random

class IntroActivity : AppCompatActivity() {

    private lateinit var spinnerLanguage: Spinner

    private val originalButtonColors = mutableMapOf<View, Int>()

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyLocale()

        setContentView(R.layout.activity_intro)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val introVisitCount = sharedPreferences.getInt("intro_visit_count", 0)

        if (introVisitCount == 0) {
            supportActionBar?.hide()

        }
        else {
            supportActionBar?.show()
            supportActionBar?.title = ""
        }

        //do while


        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        val btnContinue = findViewById<Button>(R.id.btnContinue)

        val languages = resources.getStringArray(R.array.languagesSelect)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter
        spinnerLanguage.setSelection(0)

        btnContinue.setOnClickListener {
            changeButtonBackgroundColor(it)
            val selectedPosition = spinnerLanguage.selectedItemPosition
            if (selectedPosition == 0) {
                Toast.makeText(this, "Please select a language", Toast.LENGTH_SHORT).show()
            } else {
                val newVisitCount = introVisitCount + 1
                sharedPreferences.edit().putInt("intro_visit_count", newVisitCount).apply()

                val selectedLanguage = when (selectedPosition) {
                    1 -> "tr"
                    2 -> "en"
                    else -> "en"
                }

                setLocale(selectedLanguage)

                val editor = sharedPreferences.edit()
                editor.putString("language", selectedLanguage)
                editor.putBoolean("intro_seen", true)
                editor.putLong(
                    "language_change_timestamp",
                    System.currentTimeMillis()
                )
                editor.apply()

                supportActionBar?.show()
                supportActionBar?.title = ""

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("language", selectedLanguage)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun applyLocale() {
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("language", Locale.getDefault().language) ?: Locale.getDefault().language
        setLocale(languageCode)
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onSupportNavigateUp(): Boolean {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
        return true
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun changeButtonBackgroundColor(button: View) {
        val randomColor = Constants.getRandomSoftColor(this)
        val background = button.background

        if (background is LayerDrawable) {
            for (i in 0 until background.numberOfLayers) {
                val layer = background.getDrawable(i)
                if (layer is GradientDrawable) {
                    if (!originalButtonColors.containsKey(button)) {
                        originalButtonColors[button] = (layer.color?.defaultColor ?: Color.TRANSPARENT)
                    }
                    layer.setColor(randomColor)
                    Handler(Looper.getMainLooper()).postDelayed({
                        layer.setColor(originalButtonColors[button] ?: randomColor)
                    }, 1000)
                    return
                }
            }
            Log.e("MainActivity", "GradientDrawable bulunamadı.")
        } else {
            Log.e("MainActivity", "LayerDrawable değil: ${background?.javaClass?.name}")
        }
    }


    override fun onBackPressed() {

        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val introVisitCount = sharedPreferences.getInt("intro_visit_count", 0)

        if (introVisitCount == 0) {
            finish()
        } else {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}
