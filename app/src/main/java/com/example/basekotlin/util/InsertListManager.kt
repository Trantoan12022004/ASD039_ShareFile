package com.example.basekotlin.util

import android.content.Context
import com.example.basekotlin.R
import com.example.basekotlin.model.LanguageModel
import java.util.Locale


object InsertListManager {

    fun getListLanguage(context: Context): MutableList<LanguageModel> {
        val listLanguage: MutableList<LanguageModel> = ArrayList()
        listLanguage.add(LanguageModel("English", "en", R.drawable.ic_lang_en, false))
        listLanguage.add(LanguageModel("Hindi", "hi", R.drawable.ic_lang_hi, false))
        listLanguage.add(LanguageModel("Spanish", "es", R.drawable.ic_lang_es, false))
        listLanguage.add(LanguageModel("French", "fr", R.drawable.ic_lang_fr, false))
        listLanguage.add(LanguageModel("German", "de", R.drawable.ic_lang_de, false))
        listLanguage.add(LanguageModel("Indonesian", "in", R.drawable.ic_lang_in, false))
        listLanguage.add(LanguageModel("Portuguese", "pt", R.drawable.ic_lang_pt, false))
        listLanguage.add(LanguageModel("Chinese", "zh", R.drawable.ic_lang_zh, false))

        val deviceLanguageCode = Locale.getDefault().language
        val previousLanguageCode = SystemUtil.getPreLanguage(context)
        if (SharedPreUtils.getInstance()
                .getCountOpenApp(context) > 1 && previousLanguageCode.isNotEmpty()
        ) {
            listLanguage.sortWith { lang1: LanguageModel, lang2: LanguageModel ->
                lang1.name.compareTo(
                    lang2.name, ignoreCase = true
                )
            }
            for (i in listLanguage.indices) {
                if (listLanguage[i].code == previousLanguageCode) {
                    val selectedLanguage = listLanguage.removeAt(i)
                    listLanguage.add(0, selectedLanguage)
                    break
                }
            }
        } else {
            var isDeviceLangInList = false
            for (i in listLanguage.indices) {
                if (listLanguage[i].code == deviceLanguageCode) {
                    val deviceLanguage = listLanguage.removeAt(i)
                    listLanguage.add(0, deviceLanguage)
                    isDeviceLangInList = true
                    break
                }
            }
            if (!isDeviceLangInList) {
                for (i in listLanguage.indices) {
                    if (listLanguage[i].code == "en") {
                        val englishLanguage = listLanguage.removeAt(i)
                        listLanguage.add(0, englishLanguage)
                        break
                    }
                }
            }
        }

        return listLanguage
    }


}