import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.layers.area


fun buildPlotOnDataFrameWithCompilerPlugin() {
    val dataframe = dataFrameOf(
        "years" to columnOf("2017", "2018", "2019", "2020", "2021", "2022", "2023"),
        "cost" to columnOf(56.1, 22.7, 34.7, 82.1, 53.7, 68.5, 39.9)
    )

    dataframe.plot {
        area {
            x(years)
            y(cost)
        }
    }.save("plotOnDataFrameWithCompilerPlugin.jpg")
}

fun main() = buildPlotOnDataFrameWithCompilerPlugin()