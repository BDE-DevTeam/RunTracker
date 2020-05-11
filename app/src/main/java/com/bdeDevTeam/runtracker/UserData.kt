package com.bdeDevTeam.runtracker

import com.google.android.gms.maps.model.LatLng

class UserData(map: Map<String, Any>) {
    companion object {
        const val KEY_SEX = "sex"
        const val KEY_HEIGHT = "height"
        const val KEY_WEIGHT = "weight"
        const val KEY_AGE = "age"

        fun CreateMap(sex: String, height: Int, weight: Float, age: Int): HashMap<String, Any> {

            return hashMapOf(
                    KEY_SEX to sex,
                    KEY_HEIGHT to height,
                    KEY_WEIGHT to weight,
                    KEY_AGE to age
            )
        }
    }

    private var mSex: String = " "
    private var mHeight: Int = 0
    private var mWeight: Float = 0f
    private var mAge: Int = 0

    init {
        mSex = map[KEY_SEX] as String

        when (map[KEY_HEIGHT]) {
            is Int -> mHeight = (map[KEY_HEIGHT] as Int)
            is Long -> mHeight = (map[KEY_HEIGHT] as Long).toInt()
        }

        when (map[KEY_WEIGHT]) {
            is Int -> mWeight = (map[KEY_WEIGHT] as Int).toFloat()
            is Long -> mWeight = (map[KEY_WEIGHT] as Long).toFloat()
            is Double -> mWeight = (map[KEY_WEIGHT] as Double).toFloat()
            is Float -> mWeight = map[KEY_WEIGHT] as Float
        }

        when (map[KEY_AGE]) {
            is Int -> mAge = (map[KEY_AGE] as Int)
            is Long -> mAge = (map[KEY_AGE] as Long).toInt()
        }

    }

    public fun getSex() : String {
        return mSex
    }

    public fun getHeight() : Int {
        return mHeight
    }

    public fun getWeight() : Float {
        return mWeight
    }

    public fun getAge() : Int {
        return mAge
    }
}