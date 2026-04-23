import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class FilesTestSuite {
    @Test
    fun basicCsvReadWriteTest() {
        basicCsvReadWrite()

        val file = File("jetbrains_repositories_new.csv")
        assertTrue(file.exists(), "File jetbrains_repositories_new.csv should exist")
        assertTrue(file.length() > 0, "File jetbrains_repositories_new.csv should not be empty")
    }

    @Test
    fun basicJsonReadWriteTest() {
        basicJsonReadWrite()

        val file = File("simple_new.json")
        assertTrue(file.exists(), "simple_new.json should exist")
        assertTrue(file.length() > 0, "File simple_new.json should not be empty")
    }

    @Test
    fun helloWorldTest() {
        helloWorld()
    }
}