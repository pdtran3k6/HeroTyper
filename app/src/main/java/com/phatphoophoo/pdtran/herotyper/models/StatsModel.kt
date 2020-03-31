package com.phatphoophoo.pdtran.herotyper.models

import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

//import javax.swing.UIManager.put


class StatsModel(val sp: SharedPreferences?){

    var currGameIndex : Int = 0
    var wpm: ArrayList<Int> = ArrayList()
    var keysMap: ArrayList<MutableMap<String, ArrayList<Int>>> = ArrayList()



    // Visualize typing speed over time:
    //    Key: Incremental Index: Int
    //    Value: Avg WPM (for corresponding game)
    //var typeSpeed: MutableList<Pair<Int, Int>> = mutableListOf()

    //Visualize accuracy typing speed over time:
    //    Key: Incremental Index: Int
    //    Values:
    //        Hits: Int
    //        Misses: Int
    // var : MutableList<Pair<Int, Pair<Int, Int>>> = mutableListOf()

    // Map<String, Pair<Int, Int>>



    fun read(){
//        sp!!.edit().remove("currGameIndex").commit();
//        sp!!.edit().remove("wpm").commit();
//        sp!!.edit().remove("keysMap").commit();

//        get the currGameIndex
        currGameIndex = sp!!.getInt("currGameIndex", 0)
        // TODO: Show corrupt data if return value is -1 ******
//        if (currGameIndex == -1)  true


//      get the wpm arrayList
        var gson = Gson()
        var json : String? = sp.getString("wpm", "[-1]")
        var itemType = object : TypeToken<ArrayList<Int>>() {}.type
        wpm = gson.fromJson(json, itemType)
        // TODO: Show corrupt data if return value is -1 ******
//        if (wpm[0] == -1)  true


//        get the keysMap
        json = sp.getString("keysMap", "[{\"a\":[-1]}]")
        itemType = object : TypeToken<ArrayList<MutableMap<String, ArrayList<Int>>>>() {}.type
        keysMap = gson.fromJson(json, itemType)
        // TODO: Show corrupt data if return value is -1 ******
//        if (keysMap[0] == -1)  true

        Log.e("got here ", "yes")

        var a = keysMap[0]["a"]
        Log.e("read data: ", currGameIndex.toString()  + " " + wpm.toString() + " " + (keysMap).toString() + " " + a!![0].toString() )

    }

    fun write(){
        val editor = sp!!.edit()

        currGameIndex += 1

        editor.putInt("currGameIndex", currGameIndex)
        editor.commit()

        var gson : Gson = Gson()
        var json : String = gson.toJson(wpm)
        editor.putString("wpm", json)
        editor.commit()

        json = gson.toJson(keysMap)
        editor.putString("keysMap", json)
        editor.commit()

        Log.e("wrote into mem","")

    }

}
