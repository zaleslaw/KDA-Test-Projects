import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.area
import org.jetbrains.kotlinx.kandy.letsplot.layers.bars
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import org.jetbrains.kotlinx.kandy.letsplot.layers.points
import org.jetbrains.kotlinx.kandy.letsplot.multiplot.model.PlotBunch
import java.awt.BorderLayout
import java.awt.Image
import java.nio.file.Files
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.table.AbstractTableModel

fun DataFrame<*>.swingShowWithChart(title: String = "DataFrame + Chart") {
    val df = this
    SwingUtilities.invokeLater {
        val frame = JFrame(title)
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE

        val columns = df.columns()
        val strCols = columns.filter { col -> col.values().firstOrNull() is String }
        val numCols = columns.filter { col -> col.values().firstOrNull() is Number }

        // ── Table panel ──────────────────────────────────────────────────────
        val model = object : AbstractTableModel() {
            override fun getRowCount() = df.rowsCount()
            override fun getColumnCount() = columns.size
            override fun getColumnName(col: Int) = columns[col].name()
            override fun getValueAt(row: Int, col: Int): Any? = columns[col][row]
        }
        val table = JTable(model).apply { fillsViewportHeight = true }
        val tablePanel = JPanel(BorderLayout()).apply {
            add(JLabel("  Data [${df.rowsCount()} rows × ${columns.size} cols]"), BorderLayout.NORTH)
            add(JScrollPane(table), BorderLayout.CENTER)
        }

        // ── Chart panel ──────────────────────────────────────────────────────
        val chartLabel = JLabel("Select columns and click Render", SwingConstants.CENTER)

        val xItems = (strCols.ifEmpty { numCols }).map { it.name() }.toTypedArray()
        val yItems = numCols.map { it.name() }.toTypedArray()
        val xCombo = JComboBox(xItems)
        val yCombo = JComboBox(yItems)
        val typeCombo = JComboBox(arrayOf("bars", "line", "area", "points"))

        fun renderChart() {
            val xCol = xCombo.selectedItem?.toString() ?: return
            val yCol = yCombo.selectedItem?.toString() ?: return
            val chartType = typeCombo.selectedItem?.toString() ?: "bars"
            chartLabel.icon = null
            chartLabel.text = "Rendering…"

            Thread {
                runCatching {
                    val tempFile = Files.createTempFile("kandy_", ".png").toFile()
                    df.plot {
                        when (chartType) {
                            "bars"   -> bars   { x(xCol); y(yCol) }
                            "line"   -> line   { x(xCol); y(yCol) }
                            "area"   -> area   { x(xCol); y(yCol) }
                            "points" -> points { x(xCol); y(yCol) }
                        }
                       // layout { title = "$yCol  by  $xCol ($chartType)" }
                    }.save(tempFile.absolutePath)

                    val img = ImageIO.read(tempFile)
                    val w = chartLabel.width.coerceAtLeast(500)
                    val h = chartLabel.height.coerceAtLeast(350)
                    val scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH)
                    tempFile.deleteOnExit()
                    SwingUtilities.invokeLater {
                        chartLabel.icon = ImageIcon(scaled)
                        chartLabel.text = null
                    }
                }.onFailure { e ->
                    SwingUtilities.invokeLater { chartLabel.text = "Error: ${e.message}" }
                }
            }.start()
        }

        val controlPanel = JPanel().apply {
            add(JLabel("X:")); add(xCombo)
            add(JLabel("Y:")); add(yCombo)
            add(JLabel("Type:")); add(typeCombo)
            add(JButton("Render").apply { addActionListener { renderChart() } })
        }

        val chartPanel = JPanel(BorderLayout()).apply {
            add(controlPanel, BorderLayout.NORTH)
            add(JScrollPane(chartLabel), BorderLayout.CENTER)
        }

        // ── Layout ───────────────────────────────────────────────────────────
        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, chartPanel).apply {
            dividerLocation = 420
            resizeWeight = 0.4
        }

        frame.add(split)
        frame.setSize(1100, 520)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true

        if (strCols.isNotEmpty() && numCols.isNotEmpty()) renderChart()
    }
}

fun main() {
    val sales = dataFrameOf(
        "month"    to columnOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul"),
        "revenue"  to columnOf(42000.0, 38500.0, 51000.0, 47300.0, 62000.0, 58900.0, 71200.0),
        "expenses" to columnOf(31000.0, 29000.0, 34000.0, 38000.0, 41000.0, 39500.0, 44000.0),
        "profit"   to columnOf(11000.0, 9500.0, 17000.0, 9300.0, 21000.0, 19400.0, 27200.0)
    )

    sales.swingShowWithChart("Sales Dashboard")
}
