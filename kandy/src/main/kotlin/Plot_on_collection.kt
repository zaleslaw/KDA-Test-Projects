import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.layers.area

// took from here https://kotlin.github.io/kandy/simple-area.html#-t0q1mk_9
fun buildPlotOnCollection() {
    val years = listOf("2017", "2018", "2019", "2020", "2021", "2022", "2023")
    val cost = listOf(56.1, 22.7, 34.7, 82.1, 53.7, 68.5, 39.9)

    plot {
        area {
            x(years)
            y(cost)
        }
    }.save("plotOnCollection.jpg")
}

fun main() = buildPlotOnCollection()