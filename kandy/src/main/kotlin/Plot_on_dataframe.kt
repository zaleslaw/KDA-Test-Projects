import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.area
import org.jetbrains.kotlinx.kandy.letsplot.layers.path
import org.jetbrains.kotlinx.kandy.letsplot.settings.LineType
import org.jetbrains.kotlinx.kandy.util.color.Color


fun buildPlotOnDataFrame() {
    val dataframe = dataFrameOf(
        "years" to listOf("2017", "2018", "2019", "2020", "2021", "2022", "2023"),
        "cost" to listOf(56.1, 22.7, 34.7, 82.1, 53.7, 68.5, 39.9)
    )

    dataframe.plot {
        area {
            x("years")
            y("cost")
        }
    }.save("plotOnDataFrame.jpg")
}

fun main() = buildPlotOnDataFrame()