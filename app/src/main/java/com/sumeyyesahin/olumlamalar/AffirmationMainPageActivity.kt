package com.sumeyyesahin.olumlamalar

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sumeyyesahin.olumlamalar.databinding.ActivityAffirmationMainPageBinding
import com.sumeyyesahin.olumlamalar.model.Olumlamalarlistmodel

class AffirmationMainPageActivity : AppCompatActivity() {
    private lateinit var olumlamalar: List<Olumlamalarlistmodel>
    private lateinit var binding: ActivityAffirmationMainPageBinding
    private val PREFS_NAME = "MyPrefs"
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAffirmationMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Kategori adını al
        val kategori2 = intent.getStringExtra("kategori")
        binding.textView.text = Html.fromHtml("<u>$kategori2</u>")

        val kategori = intent.getStringExtra("kategori")

        // Kullanıcının son görüntülenen olumlamadan ID'sini kontrol et
        currentIndex = getLastPosition(this, kategori!!)

        // DBHelper sınıfını kullanarak kategorinin olumlamalarını alıyoruz
        olumlamalar = DBHelper(this).getOlumlamalarByCategory(kategori)

        // currentIndex değeriyle olumlamaları güncelle
        updateAffirmationText(currentIndex)
        updateLikeButtonIcon(currentIndex)

        // İleri butonuna tıklandığında bir sonraki olumlamayı göster
        binding.ileri.setOnClickListener {
            currentIndex = (currentIndex + 1) % olumlamalar.size
            updateAffirmationText(currentIndex)
            // Son görüntülenen olumlamadan ID'sini kaydet
            saveLastPosition(this, currentIndex, kategori)
            updateLikeButtonIcon(currentIndex)
        }

        // Geri butonuna tıklandığında bir önceki olumlamayı göster
        binding.geri.setOnClickListener {
            currentIndex = (currentIndex - 1 + olumlamalar.size) % olumlamalar.size
            updateAffirmationText(currentIndex)
            // Son görüntülenen olumlamadan ID'sini kaydet
            saveLastPosition(this, currentIndex, kategori)

            updateLikeButtonIcon(currentIndex)
        }

        binding.share.setOnClickListener {
            // Olumlamayı paylaşmak
            val bitmap = takeScreenshot(binding.imageView, binding.olumlamalarTextView)

            // Share intent
            val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Olumlama", null)
            val uri = Uri.parse(path)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "image/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(shareIntent, "İçeriği Paylaş"))
        }

        binding.textView.setOnClickListener {
            // Kategori değiştiğinde currentIndex değerini güncelle
            currentIndex = getLastPosition(this, kategori)
            updateAffirmationText(currentIndex)
        }

        // Beğen butonuna tıklama işlevselliğini ekle
        binding.like.setOnClickListener {
            // Kullanıcının tıkladığı olumlama
            val clickedAffirmation = olumlamalar[currentIndex]
            // Favori durumunu tersine çevir
            clickedAffirmation.favorite = !clickedAffirmation.favorite
            // Favori durumunu güncelle

            if( clickedAffirmation.favorite){
                DBHelper(this).addAffirmationFav(clickedAffirmation,false)
                DBHelper(this).updateAffirmationFavStatus(clickedAffirmation)
            }else{
                DBHelper(this).deleteFavoriteAffirmationByCategoryAndAffirmationName("Favori Olumlamalarım",clickedAffirmation.affirmation)
                DBHelper(this).updateAffirmationFavStatus(clickedAffirmation)
            }

            // Favori butonunun ikonunu güncelle
            updateLikeButtonIcon(currentIndex)
            // Favori durumunu güncelle


        }
    }

    override fun onResume() {
        super.onResume()
        // Kategori değiştiğinde currentIndex değerini güncelle
        val kategori = intent.getStringExtra("kategori")
        currentIndex = getLastPosition(this, kategori!!)
        updateAffirmationText(currentIndex)
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
            binding.olumlamalarTextView.text = "Henüz olumlama bulunmamaktadır."
        }
    }

    // SharedPreferences'a kategoriye özgü currentIndex değerini kaydetme
    private fun saveLastPosition(context: Context, currentIndex: Int, kategori: String) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putInt(kategori, currentIndex)
        editor.apply()
    }

    // SharedPreferences'dan kategoriye özgü currentIndex değerini alma
    private fun getLastPosition(context: Context, kategori: String): Int {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getInt(kategori, 0)
    }


    // Kategori sayfasına gitmek için onClick fonksiyonu
    fun kategori(view: View){
        val intent = Intent(this, CategoryActivity::class.java)
        startActivity(intent)
    }}
