import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class KandyTestSuite {
    @Test
    fun buildPlotOnCollectionTest() {
        buildPlotOnCollection()
        val imageFile = File("lets-plot-images/plotOnCollection.jpg")
        assertTrue(imageFile.exists(), "Image file was not created")
        assertTrue(imageFile.length() > 0, "Image file is empty")
    }

    @Test
    fun buildPlotOnDataFrameTest() {
        buildPlotOnDataFrame()
        val imageFile = File("lets-plot-images/plotOnDataFrame.jpg")
        assertTrue(imageFile.exists(), "Image file was not created")
        assertTrue(imageFile.length() > 0, "Image file is empty")
    }

    @Test
    fun buildPlotOnDataFrameWithCompilerPluginTest() {
        buildPlotOnDataFrameWithCompilerPlugin()
        val imageFile = File("lets-plot-images/plotOnDataFrameWithCompilerPlugin.jpg")
        assertTrue(imageFile.exists(), "Image file was not created")
        assertTrue(imageFile.length() > 0, "Image file is empty")
    }
}