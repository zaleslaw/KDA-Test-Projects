import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.geo.GeoDataFrame
import org.jetbrains.kotlinx.dataframe.geo.io.readGeoJson
import org.jetbrains.kotlinx.dataframe.geo.io.readShapefile
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.geo.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.geo.layers.geoMap
import org.jetbrains.kotlinx.kandy.letsplot.scales.guide.model.limits
import org.jetbrains.kotlinx.kandy.letsplot.x
import org.jetbrains.kotlinx.kandy.letsplot.y

fun buildGeoGuide() {
    val usaStates = GeoDataFrame.readGeoJson("https://raw.githubusercontent.com/AndreiKingsley/datasets/refs/heads/main/USA.json")
    usaStates.df.print()

    val worldCities = GeoDataFrame.readShapefile("https://github.com/AndreiKingsley/datasets/raw/refs/heads/main/ne_10m_populated_places_simple/ne_10m_populated_places_simple.shp")
    worldCities.df.print()

    usaStates.plot {
        geoMap()
        x.axis.limits = -127..-65
        y.axis.limits = 23..50
    }.save("usaStates.jpg")
}

fun main() = buildGeoGuide()