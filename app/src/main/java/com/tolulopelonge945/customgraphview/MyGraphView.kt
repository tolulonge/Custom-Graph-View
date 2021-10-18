package com.tolulopelonge945.customgraphview

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRectF
import java.text.NumberFormat
import kotlin.math.max
import kotlin.math.min

class MyGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * Pointer values for drawing
     */
    private var transactionCurrentValue = 0 //the value of the current down location
    private val valuePointerLocation = Point()
    private var transactionMaxValue = 0f
    private var transactionMinValue = 0f
    private var valueTouched = false
    private var isDrawFirstCircle = true
    private var mCurrentDate = " "
    private var mCurrentAmt = " "
    private var mListPath: ArrayList<Path> = arrayListOf()

    companion object {
        private const val MAX_WEEKS = 4
        private var DOTTED_STROKE_WIDTH_DP = 2.px.toFloat()
        private var STROKE_WIDTH_DP = 3.px.toFloat()
        private var LIGHT_STROKE_WIDTH_DP = 1.px.toFloat()
        private var TEXT_BG_RADIUS = 2.px.toFloat()
        private var POINT_RADIUS = 6.px.toFloat()
        private var WEEK_PADDING = 30.px
        private var WEEK_DISTANCE = 20.px
        private var GRADUATIONS_BOTTOM_PADDING = 5.px
        private var GRADUATIONS_SIDE_PADDING = 30.px
    }

    private val dataPointFillPaint = Paint().apply {
        color = Color.WHITE
    }
    private val lightDataPointFillPaint = Paint().apply {
        color = Color.TRANSPARENT
    }

    private var _colorStart: Int = ContextCompat.getColor(context, R.color.colorStart)
    private var colorStart
        get() = _colorStart
        set(value) {
            _colorStart = value
            colors = intArrayOf(colorStart, colorEnd)
            invalidate()
        }

    private var _colorEnd: Int = ContextCompat.getColor(context, R.color.colorEnd)
    private var colorEnd
        get() = _colorEnd
        set(value) {
            _colorEnd = value
            colors = intArrayOf(colorStart, colorEnd)
            invalidate()
        }

    private var _lineColor: Int = ContextCompat.getColor(context, R.color.lineColor)
    private var lineColor
        get() = _lineColor
        set(value) {
            _lineColor = value
            invalidate()
        }
    private var _lightLineColor: Int = ContextCompat.getColor(context, R.color.lightlineColor)
    var lightLineColor
        get() = _lightLineColor
        set(value) {
            _lightLineColor = value
            invalidate()
        }

    private var _dottedLineColor: Int = ContextCompat.getColor(context, R.color.dottedLineColor)
    private var dottedLineColor
        get() = _dottedLineColor
        set(value) {
            _dottedLineColor = value
            invalidate()
        }

    // prepare the gradient paint
    private var _colors = intArrayOf(colorStart, colorEnd)
    private var colors
        get() = _colors
        set(value) {
            _colors = value
            invalidate()
        }

    private var _bgColor = ContextCompat.getColor(context, R.color.bg)
    var bgColor
        get() = _bgColor
        set(value) {
            _bgColor = value
            invalidate()
        }
    private var _outlineColor = ContextCompat.getColor(context, R.color.greyOutline)
    var outlineColor
        get() = _outlineColor
        set(value) {
            _outlineColor = value
            invalidate()
        }
    private val graduations: MutableList<Int> = mutableListOf()

    private var gradient: LinearGradient? = null
    private var gradientPaint: Paint? = null

    private val dottedPaint: Paint
    private val strokePaint: Paint
    private val lightStrokePaint: Paint
    private val pointPaint: Paint
    private val textPaint: TextPaint = TextPaint().apply {
        this.color = ContextCompat.getColor(context, R.color.textColor)
        this.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
    }
    private val graduationsTextPaint: TextPaint = TextPaint().apply {
        this.color = ContextCompat.getColor(context, R.color.textColor)
        this.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        isAntiAlias = true
    }

    private val textPaintBg: Paint = Paint()

    private val textRect: Rect = Rect()

    private var zeroY: Float = 0f
    private var pxPerUnit: Float = 0f

    private var graduationsDistanceScale: Float = 0f
    private var weeksDistanceScale: Float = 0f

    private var markers: List<DataPoint> = mutableListOf()
    private var weeks: MutableList<String> = mutableListOf()

    private val path: Path = Path()
    private val guidelinePath: Path = Path()

    private var chartHeight: Int = 0
    private var chartWidth: Int = 0
    private var scaleSpaceToLeaveForGraduations: Int = 0

    private var centerX = 0
    private var centerY = 0
    private val control: CurrentPositionN = CurrentPositionN(0, 0)

    init {

        mListPath = ArrayList(markers.size)
        centerX = width / 2
        centerY = height / 2
        control.x = centerX
        control.y = (centerY - 100)
        val markers = mutableListOf<DataPoint>().apply {
            this.add(DataPoint(value = 150))
            this.add(DataPoint(value = 400))
            this.add(DataPoint(value = 20))
            this.add(DataPoint(value = 100))
            this.add(DataPoint(value = 300))
        }

        val dates = mutableListOf<String>().apply {
            this.add("Oct 8")
            this.add("Oct 14")
            this.add("Oct 14")
            this.add("Oct 15")
            this.add("Oct 16")

        }
        transactionMaxValue = findMax(markers)
        transactionMinValue = findMin(markers)
        setMarkersAndDate(markers, dates)

        dottedPaint = Paint().apply {
            this.style = Paint.Style.STROKE
            this.isAntiAlias = true
            this.color = dottedLineColor
        }
        strokePaint = Paint().apply {
            this.style = Paint.Style.STROKE
            this.strokeWidth = STROKE_WIDTH_DP
            this.isAntiAlias = true
            this.color = lineColor
        }
        lightStrokePaint = Paint().apply {
            this.style = Paint.Style.STROKE
            this.strokeWidth = LIGHT_STROKE_WIDTH_DP
            this.isAntiAlias = true
            this.color = lightLineColor
        }
        pointPaint = Paint().apply {
            this.style = Paint.Style.FILL
            this.isAntiAlias = true
            this.color = lineColor
        }
        mCurrentAmt = "#${markers[0].value}.00"
        mCurrentDate = "${dates[0]},2021"
    }

    private fun setMarkersAndDate(markers: List<DataPoint>, weeks: List<String>) {
        this.weeks = weeks.toMutableList()
        this.markers = markers
        initGraduations()
    }

    private fun initGraduations() {
        val max = markers.maxByOrNull { it.value }!!
        if (max.value <= 100) {
            graduations.add(0)
            graduations.add(100)
            return
        }
        var start = max.value - max.value % 100
        while (start >= 0) {
            graduations.add(start)
            start -= 100
        }
        graduations.reverse()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var width = paddingLeft + paddingRight
        var height = paddingTop + paddingBottom

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize
        } else {
            width += 200.px
            width = max(width, suggestedMinimumWidth)
            if (widthMode == MeasureSpec.AT_MOST) width = min(widthSize, width)
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize
        } else {
            height += 200.px
            height = max(height, suggestedMinimumHeight)
            if (heightMode == MeasureSpec.AT_MOST) height = min(height, heightSize)
        }

        //compensate for the week text padding
        chartHeight = height - WEEK_PADDING
        chartWidth = width
        scaleSpaceToLeaveForGraduations = (width - 2 * GRADUATIONS_SIDE_PADDING) / markers.size
        weeksDistanceScale = (width / weeks.size).toFloat()
        graduationsDistanceScale = (height / graduations.size) * 1.072.toFloat()
        textPaint.textSize = weeksDistanceScale / weeks.size / 2f
        graduationsTextPaint.textSize = scaleSpaceToLeaveForGraduations * 0.2.toFloat()
        calcAndInvalidate()
        setMeasuredDimension(chartWidth, height + WEEK_PADDING)
    }

    private fun calcAndInvalidate() {
        calcPositions(markers)
        initGradient()
        invalidate()
    }

    private fun initGradient() {
        gradientPaint = Paint().apply {
            this.style = Paint.Style.FILL
            this.shader = gradient
            this.isAntiAlias = true
        }
        gradient = LinearGradient(
            0f, paddingTop.toFloat(), 0f, zeroY, colors, null, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        drawLineAndMarkers(canvas)
        drawWeeks(canvas)
        drawGraduations(canvas)
        drawGuidelines(canvas)
        if (valueTouched) {
            drawPointerX(canvas)
            drawPointerCircle(canvas)
            drawTextValue(canvas)
        }

        if (isDrawFirstCircle) {
            canvas.drawLine(
                markers[0].currentPos.x + GRADUATIONS_SIDE_PADDING,
                0f,
                markers[0].currentPos.x + GRADUATIONS_SIDE_PADDING,
                guidelinesY[0] - 10f,
                strokePaint
            ) //vertical line
            canvas.drawCircle(
                markers[0].currentPos.x + GRADUATIONS_SIDE_PADDING,
                markers[0].currentPos.y,
                POINT_RADIUS,
                strokePaint
            )
            canvas.drawCircle(
                markers[0].currentPos.x + GRADUATIONS_SIDE_PADDING,
                markers[0].currentPos.y, 4.px.toFloat(),
                dataPointFillPaint
            )

            canvas.drawCircle(
                markers[0].currentPos.x + GRADUATIONS_SIDE_PADDING,
                markers[0].currentPos.y + PATH_PADDING,
                POINT_RADIUS,
                lightStrokePaint
            )
            canvas.drawCircle(
                markers[0].currentPos.x + GRADUATIONS_SIDE_PADDING,
                markers[0].currentPos.y + PATH_PADDING, 4.px.toFloat(),
                lightDataPointFillPaint
            )

            drawTextValue(canvas)
        }


    }

    private val guidelinesY = arrayListOf<Float>()

    private fun drawGraduations(canvas: Canvas) {
        val x = markers.first().currentPos.x
        var step = 0f + GRADUATIONS_BOTTOM_PADDING
        for (value in graduations) {
            val y = zeroY - step
            guidelinesY.add(y)
            val formatted = NumberFormat.getIntegerInstance().format(value)
            canvas.drawText(formatted, x, y, graduationsTextPaint)
            step += graduationsDistanceScale
        }
    }

    private fun drawLineAndMarkers(canvas: Canvas) {
        val path = Path()
        val path2 = Path()
        val pathPadding = PATH_PADDING
        var previousMarker: DataPoint? = null
        for (marker in markers) {
            if (previousMarker != null) {
                // draw the line
                val p1 = previousMarker.currentPos
                val p2 = marker.currentPos
                path.moveTo(p1.x + GRADUATIONS_SIDE_PADDING, p1.y)
                path2.moveTo(p1.x + GRADUATIONS_SIDE_PADDING, p1.y + pathPadding)
                path.quadTo(
                    (p1.x + GRADUATIONS_SIDE_PADDING + p2.x) / 2,
                    p1.y / 2,
                    p2.x + GRADUATIONS_SIDE_PADDING,
                    p2.y
                )
                path2.quadTo(
                    (p1.x + GRADUATIONS_SIDE_PADDING + p2.x) / 2,
                    p1.y / 2 + pathPadding,
                    p2.x + GRADUATIONS_SIDE_PADDING,
                    p2.y + pathPadding
                )
            }
            previousMarker = marker
        }

        canvas.drawPath(path, strokePaint)
        canvas.drawPath(path2, lightStrokePaint)
    }

    private fun drawWeeks(canvas: Canvas) {
        for ((i, week) in weeks.withIndex()) {
            textPaint.getTextBounds(week, 0, week.length, textRect)
            val x = middle(i) + WEEK_DISTANCE * 2 + 35f
            val y = zeroY + textRect.height() + WEEK_DISTANCE
            val halfHeight = textRect.height() / 2f
            val left = x - WEEK_DISTANCE
            val top = y - halfHeight - WEEK_DISTANCE
            val right = x + textRect.width() + WEEK_DISTANCE
            val bottom = y + WEEK_DISTANCE
            textRect.set(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            canvas.drawText(week, x - GRADUATIONS_SIDE_PADDING, y - 40, textPaint)
        }
    }

    private fun middle(i: Int): Float {
        return (i * (chartWidth / weeks.count())).toFloat()
    }

    private fun drawGuidelines(canvas: Canvas) {
        for (i in 0 until guidelinesY.size) {
            guidelinePath.reset()
            guidelinePath.moveTo(
                paddingLeft.toFloat() + GRADUATIONS_SIDE_PADDING,
                guidelinesY[i] - 10f
            )
            guidelinePath.lineTo(width.toFloat() + GRADUATIONS_SIDE_PADDING, guidelinesY[i] - 10f)
            canvas.drawPath(guidelinePath, dottedPaint)
        }
    }

    private fun calcPositions(markers: List<DataPoint>) {
        val max = markers.maxByOrNull { it.value }!!
        val min = markers.minByOrNull { it.value }!!
        pxPerUnit = (chartHeight - paddingTop - paddingBottom) / (max.value - min.value).toFloat()
        zeroY = max.value * pxPerUnit + paddingTop

        var step =
            (chartWidth - 2 * GRADUATIONS_SIDE_PADDING - scaleSpaceToLeaveForGraduations) / (markers.size - 1)
        for ((i, marker) in markers.withIndex()) {
            if (i > 0) {
                step += 14
            }
            val x = step * i + paddingLeft
            val y = zeroY - marker.value * pxPerUnit

            marker.currentPos.x = x.toFloat()
            marker.currentPos.y = y
        }
    }

    private fun setPointerLocation(event: MotionEvent) {
        var index = (event.x * (markers.size - 1.0f) / width.toFloat() + 1.2f).toInt()
        index -= 1
        Log.d("Pointer Location", "setPointerLocation: $index")
        if (index < 0) {
            index = 0
        }
        if (index > markers.size - 1) {
            index = markers.size - 1
        }
        mCurrentAmt = "#${markers[index].value}.00"
        mCurrentDate = "${weeks[index]},2021"
        transactionCurrentValue = markers[index].value

        if (index == 0) {
            valuePointerLocation.set(
                markers[index].currentPos.x.toInt() + GRADUATIONS_SIDE_PADDING,
                markers[index].currentPos.y.toInt()
            )

        } else {
            valuePointerLocation.set(
                markers[index].currentPos.x.toInt() + GRADUATIONS_SIDE_PADDING,
                markers[index].currentPos.y.toInt()
            )

        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                setPointerLocation(event)
                valueTouched = true
                isDrawFirstCircle = false
                invalidate()
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                valueTouched = true
                invalidate()
            }
        }
        return true
    }

    private fun drawTextValue(canvas: Canvas) {

        textPaint.getTextBounds("Week 1", 0, 6, textRect)
        var x = valuePointerLocation.x + GRADUATIONS_SIDE_PADDING
        if (valueTouched) x -= 100
        if (valuePointerLocation.x == 988) x -= 130
        val y = markers[0].currentPos.x + 30
        val y2 = markers[0].currentPos.x + 65
        val halfHeight = textRect.height() / 2f
        val left = x - WEEK_DISTANCE
        val right = x + textRect.width() + WEEK_DISTANCE
        val bottom = y + WEEK_DISTANCE
        textRect.set(left, 2, right, bottom.toInt())
        textPaintBg.color = bgColor
        textPaintBg.style = Paint.Style.FILL
        canvas.drawRoundRect(textRect.toRectF(), TEXT_BG_RADIUS, TEXT_BG_RADIUS, textPaintBg)
        textPaintBg.color = outlineColor
        textPaintBg.style = Paint.Style.STROKE
        textPaintBg.strokeWidth = 2.px.toFloat()
        canvas.drawRoundRect(textRect.toRectF(), TEXT_BG_RADIUS, TEXT_BG_RADIUS, textPaintBg)
        canvas.drawText(mCurrentDate, x - 35f, y, textPaint)
        canvas.drawText(mCurrentAmt, x - 35f, y2, textPaint)
    }

    private fun drawPointerX(canvas: Canvas) {
        canvas.drawLine(
            valuePointerLocation.x.toFloat(),
            0f,
            valuePointerLocation.x.toFloat(),
            guidelinesY[0] - 10f,
            strokePaint
        ) //vertical line
    }

    private fun drawPointerCircle(canvas: Canvas) {
        canvas.drawCircle(
            valuePointerLocation.x.toFloat(),
            valuePointerLocation.y.toFloat(),
            POINT_RADIUS,
            strokePaint
        )
        canvas.drawCircle(
            valuePointerLocation.x.toFloat(),
            valuePointerLocation.y.toFloat(),
            4.px.toFloat(),
            dataPointFillPaint
        )
        canvas.drawCircle(
            valuePointerLocation.x.toFloat(),
            valuePointerLocation.y.toFloat() + PATH_PADDING,
            POINT_RADIUS,
            lightStrokePaint
        )
        canvas.drawCircle(
            valuePointerLocation.x.toFloat(),
            valuePointerLocation.y.toFloat() + PATH_PADDING,
            4.px.toFloat(),
            lightDataPointFillPaint
        )

    }
}