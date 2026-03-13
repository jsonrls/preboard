package com.pbec.preboardexamchecker.data.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ListLongConverter {
    @TypeConverter
    fun fromListLong(list: List<Long>?): String? {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun toListLong(json: String?): List<Long>? {
        val type = object : TypeToken<List<Long>>() {}.type
        return Gson().fromJson(json, type)
    }
}