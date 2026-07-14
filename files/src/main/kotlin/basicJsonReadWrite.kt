import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.head
import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.io.readJson
import org.jetbrains.kotlinx.dataframe.io.writeJson

fun basicJsonReadWrite() {
    val url = object {}.javaClass.getResource("/simple.json")!!
    val df = DataFrame.readJson(url)
    df.head(5).print()

    df.writeJson("simple_new.json")
}

fun main(): Unit = basicJsonReadWrite()