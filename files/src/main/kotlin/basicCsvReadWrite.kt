import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.head
import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.io.readCsv
import org.jetbrains.kotlinx.dataframe.io.writeCsv

fun basicCsvReadWrite() {
    val repos = DataFrame.readCsv("https://raw.githubusercontent.com/Kotlin/dataframe/master/data/jetbrains_repositories.csv")

    repos.head(5).print()

    repos.writeCsv("jetbrains_repositories_new.csv")
}

fun main(): Unit = basicCsvReadWrite()