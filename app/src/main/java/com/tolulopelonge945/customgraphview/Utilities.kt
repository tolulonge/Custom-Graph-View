package com.tolulopelonge945.customgraphview

import android.content.res.Resources

fun findMax(dataX: MutableList<DataPoint>): Float {
    val data = arrayListOf<Float>()
    dataX.forEach {
        data.add(it.value.toFloat())
    }
    var max = Float.MIN_VALUE
    for (aData in data) {
        if (aData > max) {
            max = aData
        }
    }
    return max
}

fun findMin(dataX: MutableList<DataPoint>): Float {
    val data = arrayListOf<Float>()
    dataX.forEach {
        data.add(it.value.toFloat())
    }
    var min = Float.MAX_VALUE
    for (aData in data) {
        if (aData < min) {
            min = aData
        }
    }
    return min
}
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()