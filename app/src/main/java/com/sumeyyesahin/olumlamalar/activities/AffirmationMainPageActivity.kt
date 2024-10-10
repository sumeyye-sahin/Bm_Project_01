package com.sumeyyesahin.olumlamalar.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.sumeyyesahin.olumlamalar.helpers.DBHelper
import com.sumeyyesahin.olumlamalar.R
import com.sumeyyesahin.olumlamalar.databinding.ActivityAffirmationMainPageBinding
import com.sumeyyesahin.olumlamalar.model.AffirmationsListModel
import com.sumeyyesahin.olumlamalar.utils.GetSetUserLanguage
import com.sumeyyesahin.olumlamalar.utils.GetSetUserLanguage.setUserLanguage
import java.io.File
import java.io.FileOutputStream

class AffirmationMainPageActivity : AppCompatActivity() {
    private lateinit var olumlamalar: List<AffirmationsListModel>
    private lateinit var binding: ActivityAffirmationMainPageBinding
    private val PREFS_NAME = "MyPrefs"
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAffirmationMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val kategori2 = intent.getStringExtra("kategori")
        binding.textView.text = Html.fromHtml("<u>$kategori2</u>")

        val kategori = intent.getStringExtra("kategori")
        val language = GetSetUserLanguage.getUserLanguage(this)
        setUserLanguage(this, language)

        currentIndex = getLastPosition(this, kategori!!)

        olumlamalar = DBHelper(this).getOlumlamalarByCategoryAndLanguage(kategori, language)

        updateUI()

        setDeleteButtonVisibility(kategori, language)

        binding.ileri.setOnClickListener {
            binding.ileri.alpha = 0.5f
            binding.ileri.postDelayed({
                binding.ileri.alpha = 1f
            }, 300)
            currentIndex = (currentIndex + 1) % olumlamalar.size
            updateAffirmationText(currentIndex)

            saveLastPosition(this, currentIndex, kategori)
            updateUI()
        }

        binding.geri.setOnClickListener {
            binding.geri.alpha = 0.5f
            binding.geri.postDelayed({
                binding.geri.alpha = 1f
            }, 300)

            currentIndex = (currentIndex - 1 + olumlamalar.size) % olumlamalar.size
            updateAffirmationText(currentIndex)
            // Son görüntülenen olumlamadan ID'sini kaydet
            saveLastPosition(this, currentIndex, kategori)
            updateUI()
        }

        binding.delete.setOnClickListener {
            if (olumlamalar.isNotEmpty()) {

                binding.delete.alpha = 0.5f
                binding.delete.postDelayed({
                    binding.delete.alpha = 1f
                }, 300)

                DBHelper(this).deleteAffirmation(olumlamalar[currentIndex].id)
                olumlamalar = DBHelper(this).getOlumlamalarByCategoryAndLanguage(kategori, language)
                currentIndex = 0
                updateUI()
            }
        }

        binding.share.setOnClickListener {

            binding.share.alpha = 0.5f
            binding.share.postDelayed({
                binding.share.alpha = 1f
            }, 300)

            // Olumlamayı paylaşmak
            val bitmap = takeScreenshot(binding.affirmationBackground, binding.olumlamalarTextView)

            // Geçici dosya oluşturma
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "shared_image.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()

            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

            // Share intent
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "image/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Share Content"))

            // Paylaşımdan sonra dosyayı silme
            shareIntent.resolveActivity(packageManager)?.also {
                file.deleteOnExit()
            }
        }

        binding.textView.setOnClickListener {

            currentIndex = getLastPosition(this, kategori)
            updateAffirmationText(currentIndex)
            updateUI()
        }


        binding.like.setOnClickListener {

            val clickedAffirmation = olumlamalar[currentIndex]

            clickedAffirmation.favorite = !clickedAffirmation.favorite


            if (clickedAffirmation.favorite) {
                DBHelper(this).addAffirmationFav(clickedAffirmation, false, language)
                DBHelper(this).updateAffirmationFavStatus(clickedAffirmation)
            } else {
                DBHelper(this).deleteFavoriteAffirmationByCategoryAndAffirmationName("My Affirmations", clickedAffirmation.affirmation)
                DBHelper(this).updateAffirmationFavStatus(clickedAffirmation)
            }


            updateLikeButtonIcon(currentIndex)
        }

        binding.addbutton.setOnClickListener {
            binding.addbutton.alpha = 0.5f
            binding.addbutton.postDelayed({
                binding.addbutton.alpha = 1f
            }, 300)
            val intent = Intent(this, AddAffirmationActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_ADD_AFFIRMATION)
        }
    }

    override fun onResume() {
        super.onResume()

        val kategori = intent.getStringExtra("kategori")
        val language = GetSetUserLanguage.getUserLanguage(this)
        olumlamalar = DBHelper(this).getOlumlamalarByCategoryAndLanguage(kategori!!, language)


        if (olumlamalar.isNotEmpty()) {
            currentIndex = getLastPosition(this, kategori).coerceAtMost(olumlamalar.size - 1)
        } else {
            currentIndex = 0 //
        }


        setDeleteButtonVisibility(kategori, language)

        updateUI()
    }

    override fun onBackPressed() {
        val intent = Intent(this, CategoryActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ADD_AFFIRMATION && resultCode == RESULT_OK) {

            val kategori = intent.getStringExtra("kategori")!!
            olumlamalar = DBHelper(this).getOlumlamalarByCategoryAndLanguage(kategori, GetSetUserLanguage.getUserLanguage(this))
            updateUI()
        }
    }



    private fun updateUI() {

        if (olumlamalar.isNotEmpty()) {
            updateAffirmationText(currentIndex)
            updateLikeButtonIcon(currentIndex)
            enableButtons(true)
        } else {
            binding.olumlamalarTextView.text = getString(R.string.add_buton)
            binding.like.background = getDrawable(R.drawable.baseline_favorite_border_24)
            binding.like.visibility = View.INVISIBLE
            binding.delete.visibility = View.INVISIBLE
            binding.ileri.visibility = View.INVISIBLE
            binding.geri.visibility = View.INVISIBLE
            binding.share.visibility = View.INVISIBLE

            binding.like.isClickable = false
            binding.delete.isClickable = false
            binding.ileri.isClickable = false
            binding.geri.isClickable = false
            binding.share.isClickable = false
        }


        val kategori = intent.getStringExtra("kategori") ?: ""
        Log.d("Kategori", "Kategori: $kategori") // Kategori adını loglayın

        val backgroundResource = when (kategori) {
            getString(R.string.general_affirmations) -> R.drawable.genel_background
            getString(R.string.body_affirmations) -> R.drawable.body_background3
            getString(R.string.faith_affirmations) -> R.drawable.inanc_background
            getString(R.string.bad_days_affirmations) -> R.drawable.zor_background3
            getString(R.string.love_affirmations) -> R.drawable.love_background
            getString(R.string.self_value_affirmations) -> R.drawable.ozdeger_background
            getString(R.string.stress_affirmations) -> R.drawable.stress_background
            getString(R.string.positive_thought_affirmations) -> R.drawable.body_background2
            getString(R.string.success_affirmations) -> R.drawable.basari_background
            getString(R.string.personal_development_affirmations) -> R.drawable.kisisel_background
            getString(R.string.time_management_affirmations) -> R.drawable.zaman_background
            getString(R.string.relationship_affirmations) -> R.drawable.iliski_background
            getString(R.string.prayer_affirmations) -> R.drawable.dua_background
            getString(R.string.add_affirmation_title) -> R.drawable.kendi_background
            else -> R.drawable.genel_background
        }

        Log.d("Arka Plan", "Arka Plan Resource ID: $backgroundResource")
        binding.affirmationBackground.setImageResource(backgroundResource)



    }

    private fun enableButtons(enable: Boolean) {
        binding.ileri.isClickable = enable
        binding.geri.isClickable = enable
        binding.share.isClickable = enable
        binding.like.isClickable = enable
        binding.delete.isClickable = enable

        binding.like.visibility = View.VISIBLE
        binding.ileri.visibility = View.VISIBLE
        binding.geri.visibility = View.VISIBLE
        binding.share.visibility = View.VISIBLE

    }

    private fun setDeleteButtonVisibility(kategori: String, language: String) {
        val myAffirmationsCategory = if (language == "tr") {
            "Kendi Olumlamalarım"
        } else {
            "My Affirmations"
        }
        if (kategori == myAffirmationsCategory) {
            binding.delete.visibility = View.VISIBLE
        } else {
            binding.delete.visibility = View.GONE
        }
    }

    private fun updateLikeButtonIcon(index: Int) {
        val likeButtonIcon = if (olumlamalar[index].favorite) {
            R.drawable.baseline_favorite
        } else {
            R.drawable.baseline_favorite_border_24
        }
        binding.like.background = getDrawable(likeButtonIcon)
    }

    private fun takeScreenshot(imageView: ImageView, textView: TextView): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(imageView.width, imageView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        imageView.draw(canvas)

        val (xDelta, yDelta) = calculateTextViewPosition(imageView, textView)
        canvas.translate(xDelta, yDelta)
        textView.draw(canvas)

        return returnedBitmap
    }

    private fun calculateTextViewPosition(imageView: ImageView, textView: TextView): Pair<Float, Float> {
        val xDelta = (imageView.width - textView.width) / 2f
        val yDelta = (imageView.height - textView.height) / 2f

        return Pair(xDelta, yDelta)
    }

    private fun updateAffirmationText(currentIndex: Int) {
        if (olumlamalar.isNotEmpty()) {
            binding.olumlamalarTextView.text = olumlamalar[currentIndex].affirmation
        } else {
            binding.olumlamalarTextView.text = "getResources().getString(R.string.add_buton)"
        }
    }


    private fun saveLastPosition(context: Context, currentIndex: Int, kategori: String) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putInt(kategori, currentIndex)
        editor.apply()
    }


    private fun getLastPosition(context: Context, kategori: String): Int {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getInt(kategori, 0)
    }


    fun kategori(view: View) {
        view.alpha = 0.5f
        view.postDelayed({
            view.alpha = 1f
        }, 300)
        val intent = Intent(this, CategoryActivity::class.java)
        startActivity(intent)
        finish()
    }


    companion object {
        private const val REQUEST_CODE_ADD_AFFIRMATION = 1
    }
}

