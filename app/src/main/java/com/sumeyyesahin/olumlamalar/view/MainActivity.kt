package com.sumeyyesahin.olumlamalar.view

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sumeyyesahin.olumlamalar.worker.NotificationWorker
import com.sumeyyesahin.olumlamalar.R
import com.sumeyyesahin.olumlamalar.databinding.ActivityMainBinding
import com.sumeyyesahin.olumlamalar.helpers.DBHelper
import com.sumeyyesahin.olumlamalar.utils.Constants
import java.util.Locale
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentLanguage: String = "en"
    private var currentIndex = 0
    private val flowerImages = arrayOf(
        R.drawable.mainflowerlavanta,
        R.drawable.mainflowerpink,
        R.drawable.mainflowerturuncu,
        R.drawable.mainflowerturkuaz,
        R.drawable.mainfloweryellow,
        R.drawable.mainflowerkirmizi,
        R.drawable.mainflower
    )

    private val originalButtonColors = mutableMapOf<View, Int>()
    private var batteryOptimizationDialog: AlertDialog? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        currentLanguage = intent.getStringExtra("language") ?: sharedPreferences.getString("language", "en") ?: "en"
        setLocale(currentLanguage)

        val savedInstanceStateLanguage = savedInstanceState?.getString("language")
        if (savedInstanceStateLanguage != null && savedInstanceStateLanguage != currentLanguage) {
            currentLanguage = savedInstanceStateLanguage
            setLocale(currentLanguage)
            recreate()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isFirstDescriptionSeen = sharedPreferences.getBoolean("first_description_seen", false)
        val isIntroSeen = sharedPreferences.getBoolean("intro_seen", false)
        if (!isFirstDescriptionSeen) {
            // İlk defa açılıyorsa FirstDescriptionPage'i göster
            val intent = Intent(this, FirstDescriptionPage::class.java)
            startActivity(intent)
            finish()
            return
        } else if (!isIntroSeen) {
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        requestPermissions()
        createNotificationChannel(this)
        scheduleNotifications(currentLanguage)

        binding.imageView2.setOnClickListener {
            binding.imageView2.setImageResource(flowerImages[currentIndex])
            currentIndex++
            if (currentIndex >= flowerImages.size) {
                currentIndex = 0
            }
        }

        val dbHelper = DBHelper(this)
        dbHelper.readableDatabase

        binding.buttonnefes.setOnClickListener {
            Constants.changeButtonBackgroundColor(it)
            val breathingIntroSeen = sharedPreferences.getBoolean("breathing_intro_seen", false)

            if (breathingIntroSeen) {
                val intent = Intent(this, BreathActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this, BreathingIntroActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        binding.buttonSettings.setOnClickListener {
            Constants.changeButtonBackgroundColor(it)
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.buttonkategori.setOnClickListener {
            Constants.changeButtonBackgroundColor(it)
            val intent = Intent(this, CategoryActivity::class.java)
            intent.putExtra("language", currentLanguage)
            startActivity(intent)
            finish()
        }

    }




    private fun requestPermissions() {
    // Request notification permission for Android 13 and above
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = packageName
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }
}
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("CHANNEL_ID", "Random Notification", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Channel for random category notifications"
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleNotifications(language: String) {
        val inputData = Data.Builder()
            .putString("language", language)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyNotificationWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
    private fun updateNotificationsWithNewLanguage(language: String) { //bunu en son koydum olmazsa sil
        WorkManager.getInstance(this).cancelUniqueWork("DailyNotificationWork")

        scheduleNotifications(language)
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val language = intent.getStringExtra("language") ?: sharedPreferences.getString("language", "en") ?: "en"
        if (language != currentLanguage) {
            currentLanguage = language
            setLocale(currentLanguage)
            updateNotificationsWithNewLanguage(currentLanguage) //bunu en son koydum olmazsa sil
            recreate() // Aktiviteyi yeniden başlat
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("language", currentLanguage)
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryOptimizationDialog?.dismiss()
    }
}
