import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.filter
import org.jetbrains.kotlinx.dataframe.api.sortByDesc

fun helloWorld() {
    val df = dataFrameOf(
        "name" to listOf("Alice", "Anton", "Bob"),
        "age" to listOf(25, 30, 15)
    )

    val res = df.filter { "name"<String>().startsWith("A") }
        .sortByDesc("age")

    println(res)
}

fun main(): Unit = helloWorld()