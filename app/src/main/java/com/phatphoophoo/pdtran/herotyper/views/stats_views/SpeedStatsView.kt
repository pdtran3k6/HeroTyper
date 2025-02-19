package com.phatphoophoo.pdtran.herotyper.views.stats_views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.phatphoophoo.pdtran.herotyper.R
import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter
import lecho.lib.hellocharts.model.*
import lecho.lib.hellocharts.view.LineChartView


class SpeedStatsView: LinearLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, attributeSetId: Int) : super(context, attrs, attributeSetId)

    private val data: LineChartData = LineChartData()

    init {
        inflate(context, R.layout.speed_stats_layout, this)

        val axisX = Axis()
        val axisY = Axis().setHasLines(true)
        axisX.name = "Game Number"
        axisY.name = "Average WPM"

        val xFormater = SimpleAxisValueFormatter()
        xFormater.decimalDigitsNumber = 0
        axisX.formatter = xFormater

        data.axisXBottom = axisX
        data.axisYLeft = axisY
    }

    fun createGraph(speedStats: List<Int>) {
        if(speedStats.size <= 1) {
            Log.e("Speed Stats View", "Empty speedStats received")
            return
        }
        // Special formatting based off number of games recorded so far
        if (speedStats.size < 10){
            data.axisXBottom.isAutoGenerated = false
            data.axisXBottom.values = (1..speedStats.size).map { AxisValue(it.toFloat()) }
        }

        val points = speedStats.mapIndexed{idx, value -> PointValue(idx.toFloat() + 1, value.toFloat())}

        val line = Line(points)
        line.color = R.color.colorPrimary
        val lines = arrayListOf(line)

        data.lines = lines

        val chart = findViewById<LineChartView>(R.id.chart)
        chart.lineChartData = data

        //Remove no data view
        val noDataView = findViewById<TextView>(R.id.no_data_view)
        noDataView.visibility = View.GONE

        //Make chart visible
        chart.visibility = View.VISIBLE
    }
}