package com.sumeyyesahin.olumlamalar.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sumeyyesahin.olumlamalar.R
import com.sumeyyesahin.olumlamalar.helpers.DBHelper
import com.sumeyyesahin.olumlamalar.model.AffirmationsListModel
import com.sumeyyesahin.olumlamalar.utils.GetSetUserLanguage

class AffirmationMainPageViewModel(application: Application) : AndroidViewModel(application) {
    private val _category = MutableLiveData<String>()
    val category: LiveData<String> get() = _category
    private val PREFS_NAME = "MyPrefs"
    private val _language = MutableLiveData<String>()
    val language: LiveData<String> get() = _language

    private val _olumlamalar = MutableLiveData<List<AffirmationsListModel>>()
    val olumlamalar: LiveData<List<AffirmationsListModel>> get() = _olumlamalar
    private val _currentIndex = MutableLiveData<Int>()
    val currentIndex: LiveData<Int> get() = _currentIndex
    private val _isMyAffirmationCategory = MutableLiveData<Boolean>()
    val isMyAffirmationCategory: LiveData<Boolean> get() = _isMyAffirmationCategory


    private val dbHelper = DBHelper(application)



    fun initialize(category: String, context: Context) {
        _category.value = category
        _language.value = GetSetUserLanguage.getUserLanguage(context)
        _isMyAffirmationCategory.value = category == context.getString(R.string.add_affirmation_title)
        val savedIndex = getLastPosition(context, category)
        _currentIndex.value = savedIndex
        loadAffirmations(category, _language.value!!)
    }

    fun loadAffirmations(category: String, language: String) {
        val context = getApplication<Application>()
        val affirmations = if (category == context.getString(R.string.favorite_affirmations)) {
            dbHelper.getFavoriteAffirmationsByLanguage(language)
        } else {
            dbHelper.getOlumlamalarByCategoryAndLanguage(category, language)
        }
        _olumlamalar.value = affirmations
    }


    fun updateCurrentIndex(context: Context, index: Int, category: String) {
        _currentIndex.value = index
        saveLastPosition(context, index, category)
    }

    private fun saveLastPosition(context: Context, index: Int, category: String) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt(category, index).apply()
    }

    private fun getLastPosition(context: Context, category: String): Int {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getInt(category, 0)
    }

    fun deleteAffirmation(affirmation: AffirmationsListModel) {
        dbHelper.deleteAffirmation(affirmation.id)
        _olumlamalar.value = _olumlamalar.value?.filter { it.id != affirmation.id }
    }

    fun toggleFavorite(affirmation: AffirmationsListModel) {
        affirmation.favorite = !affirmation.favorite
        dbHelper.updateAffirmationFavStatus(affirmation)
        val currentCategory = _category.value ?: ""
        val currentLanguage = _language.value ?: "en"

        if (affirmation.favorite) {
            dbHelper.addAffirmationFav(affirmation, true, currentLanguage)
        } else {
            val context = getApplication<Application>()
            dbHelper.deleteFavoriteAffirmationByCategoryAndAffirmationName(
                context.getString(R.string.favorite_affirmations),
                affirmation.affirmation
            )

        }

        // Affirmasyonları yeniden yükle
        loadAffirmations(currentCategory, currentLanguage)
        _olumlamalar.value = _olumlamalar.value?.toMutableList()
    }

}
