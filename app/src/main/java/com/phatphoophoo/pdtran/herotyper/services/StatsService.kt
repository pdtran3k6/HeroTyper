package com.phatphoophoo.pdtran.herotyper.services

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.phatphoophoo.pdtran.herotyper.activities.MainMenuActivity
import com.phatphoophoo.pdtran.herotyper.activities.StatsActivity
import com.phatphoophoo.pdtran.herotyper.models.StatsModel


object StatsService {
    lateinit var sp: SharedPreferences
    lateinit var statsModel: StatsModel

//    fun initService(context: Context): StatsService {
    fun initService(mainMenuActivity: MainMenuActivity): StatsService {
//        this.sp = PreferenceManager.getDefaultSharedPreferences(context)
        this.sp = mainMenuActivity?.getPreferences(Context.MODE_PRIVATE)
        this.statsModel = StatsModel(sp)

//       read the data right at the beginning. !!
        statsModel = StatsModel(sp)

        //Test
        //testing
//        statsModel.currGameIndex = 1
//        statsModel.wpm.add(1)
//
//        val map = mutableMapOf("a" to arrayListOf(1, 2))
//        statsModel.keysMap.add(map)
//
//        statsModel.write()
        statsModel.read()
        return this
    }

    fun updateWpm(n: Int) {
        Log.e("updating wpm", n.toString())
        statsModel.wpm.add(n)
    }

    fun updateKeysMap(m : MutableMap<String, ArrayList<Int>>) {
        statsModel.keysMap.add(m)
    }

    // write data
    fun write() {
        statsModel.write()
    }
}