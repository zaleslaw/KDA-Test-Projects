import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class GeoTestSuite {
    @Test
    fun buildGeoGuideTest() {
        buildGeoGuide()

        val file = File("lets-plot-images/usaStates.jpg")
        assertTrue(file.exists(), "File lets-plot-images/usaStates.jpg should exist")
        assertTrue(file.length() > 0, "File lets-plot-images/usaStates.jpg should not be empty")
    }
}
