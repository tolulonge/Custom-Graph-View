package com.tolulopelonge945.customgraphview

data class DataPoint(val currentPos: CurrentPosition = CurrentPosition(0f, 0f), val value: Int = 0)

data class CurrentPosition(var x: Float, var y: Float)

data class CurrentPositionN(var x : Int, var y : Int)