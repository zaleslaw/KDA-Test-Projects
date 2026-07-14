import com.formdev.flatlaf.FlatDarkLaf
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import java.awt.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import javax.script.ScriptEngineManager
import javax.swing.*
import javax.swing.table.AbstractTableModel

private val cellCounter = AtomicInteger(0)

private val TEMPLATE_TABLE = """
import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf

dataFrameOf(
    "name"   to columnOf("Alice", "Bob", "Charlie", "Diana"),
    "age"    to columnOf(30, 25, 35, 28),
    "salary" to columnOf(75000.0, 60000.0, 90000.0, 80000.0)
)
""".trimIndent()

private val TEMPLATE_CHART = """
import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.layers.bars

val df = dataFrameOf(
    "month"   to columnOf("Jan", "Feb", "Mar", "Apr", "May", "Jun"),
    "revenue" to columnOf(42.0, 38.5, 51.0, 47.3, 62.0, 58.9)
)

df.plot {
    bars { x("month"); y("revenue") }
}.save(outputPath)
""".trimIndent()

private val TEMPLATE_NEW = """
import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf

dataFrameOf(
    "x" to columnOf(1, 2, 3, 4, 5),
    "y" to columnOf(10.0, 25.0, 18.0, 30.0, 22.0)
)
""".trimIndent()

// ── Notebook cell ────────────────────────────────────────────────────────────

private class NotebookCell(
    initialCode: String,
    cellNum: Int,
    onRemove: (NotebookCell) -> Unit
) : JPanel(BorderLayout()) {

    val codeArea = JTextArea(initialCode).apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 13)
        tabSize = 4
        rows = 6
    }

    private val outputPanel = JPanel(BorderLayout()).apply { isVisible = false }

    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, Color(0x4B6EAF)),
            BorderFactory.createEmptyBorder(8, 8, 6, 8)
        )

        val inLabel = JLabel("  In [$cellNum]").apply {
            font = font.deriveFont(Font.BOLD, 12f)
            foreground = Color(0x6897BB)
        }
        val btnTable = JButton("▶ Table").apply {
            putClientProperty("JButton.buttonType", "default")
            toolTipText = "Execute — last expression shown as DataFrame table"
            addActionListener { executeAsTable() }
        }
        val btnChart = JButton("▶ Chart").apply {
            toolTipText = "Execute — use .save(outputPath) to render a Kandy chart"
            addActionListener { executeAsChart() }
        }
        val btnRemove = JButton("✕").apply {
            putClientProperty("JButton.buttonType", "borderless")
            foreground = Color(0xCC5555)
            toolTipText = "Remove cell"
            addActionListener { onRemove(this@NotebookCell) }
        }

        val toolbar = JPanel(BorderLayout()).apply {
            val left = JPanel(FlowLayout(FlowLayout.LEFT, 6, 2)).apply {
                add(inLabel); add(btnTable); add(btnChart)
            }
            add(left, BorderLayout.WEST)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 2)).apply { add(btnRemove) }, BorderLayout.EAST)
        }

        outputPanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color(0x4A4A4A)),
            BorderFactory.createEmptyBorder(8, 0, 4, 0)
        )

        add(toolbar, BorderLayout.NORTH)
        add(JScrollPane(codeArea).apply {
            border = BorderFactory.createLineBorder(Color(0x4A4A4A))
            minimumSize = Dimension(200, 80)
        }, BorderLayout.CENTER)
        add(outputPanel, BorderLayout.SOUTH)
    }

    // ── Output helpers ───────────────────────────────────────────────────────

    private fun setOutput(component: JComponent?) {
        outputPanel.removeAll()
        outputPanel.isVisible = component != null
        if (component != null) outputPanel.add(component, BorderLayout.CENTER)
        revalidate(); repaint()
    }

    private fun showRunning() {
        val p = JProgressBar().apply {
            isIndeterminate = true; isStringPainted = true; string = "Running…"
        }
        outputPanel.removeAll()
        outputPanel.add(p, BorderLayout.NORTH)
        outputPanel.isVisible = true
        revalidate(); repaint()
    }

    private fun buildTable(df: DataFrame<*>): JComponent {
        val cols = df.columns()
        val displayRows = df.rowsCount().coerceAtMost(50)
        val model = object : AbstractTableModel() {
            override fun getRowCount() = displayRows
            override fun getColumnCount() = cols.size
            override fun getColumnName(c: Int) = cols[c].name()
            override fun getValueAt(r: Int, c: Int): Any? = cols[c][r]
        }
        val table = JTable(model).apply {
            rowHeight = 24; showHorizontalLines = false; showVerticalLines = false
            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        }
        val visibleRows = displayRows.coerceAtMost(8)
        val scroll = JScrollPane(table).apply {
            preferredSize = Dimension(800, visibleRows * 26 + table.tableHeader.preferredSize.height + 4)
            border = BorderFactory.createLineBorder(Color(0x4A4A4A))
        }
        return JPanel(BorderLayout()).apply {
            add(scroll, BorderLayout.CENTER)
            if (df.rowsCount() > 50) add(JLabel("  Showing 50 of ${df.rowsCount()} rows").apply {
                font = font.deriveFont(11f); foreground = Color(0x888888)
                border = BorderFactory.createEmptyBorder(4, 0, 0, 0)
            }, BorderLayout.SOUTH)
        }
    }

    private fun buildError(msg: String): JComponent =
        JTextArea("⚠  $msg").apply {
            isEditable = false
            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            foreground = Color(0xFF7777); background = Color(0x3C1010)
            border = BorderFactory.createEmptyBorder(8, 10, 8, 10)
            lineWrap = true; wrapStyleWord = true
        }

    // ── Execute ──────────────────────────────────────────────────────────────

    fun executeAsTable() {
        showRunning()
        val origOut = System.out
        Thread {
            System.setOut(PrintStream(ByteArrayOutputStream()))
            val result = runCatching {
                val engine = ScriptEngineManager().getEngineByExtension("kts")
                    ?: error("Kotlin scripting engine not found — check kotlin-scripting-jsr223 dependency")
                engine.eval(codeArea.text)
            }
            System.setOut(origOut)
            SwingUtilities.invokeLater {
                result
                    .onSuccess { v -> setOutput(if (v is DataFrame<*>) buildTable(v) else JLabel("  $v")) }
                    .onFailure { e -> setOutput(buildError("${e::class.simpleName}: ${e.message}")) }
            }
        }.start()
    }

    fun executeAsChart() {
        showRunning()
        val tempFile = Files.createTempFile("kandy_nb_", ".png").toFile()
        val origOut = System.out
        Thread {
            System.setOut(PrintStream(ByteArrayOutputStream()))
            val result = runCatching {
                val engine = ScriptEngineManager().getEngineByExtension("kts")
                    ?: error("Kotlin scripting engine not found")
                engine.put("outputPath", tempFile.absolutePath)
                engine.eval(codeArea.text)
            }
            System.setOut(origOut)
            SwingUtilities.invokeLater {
                if (tempFile.exists() && tempFile.length() > 0) {
                    val img = ImageIO.read(tempFile)
                    val targetW = img.width.coerceAtMost(860)
                    val scaled = img.getScaledInstance(targetW, (img.height * targetW.toDouble() / img.width).toInt(), Image.SCALE_SMOOTH)
                    setOutput(JLabel(ImageIcon(scaled)).apply { border = BorderFactory.createEmptyBorder(4, 0, 4, 0) })
                    tempFile.deleteOnExit()
                } else {
                    val msg = result.exceptionOrNull()?.let { "${it::class.simpleName}: ${it.message}" }
                        ?: "No chart — did you call .save(outputPath)?"
                    setOutput(buildError(msg))
                }
            }
        }.start()
    }
}

// ── Notebook window ──────────────────────────────────────────────────────────

fun showKandyNotebook(title: String = "KDF Notebook") {
    FlatDarkLaf.setup()
    UIManager.put("Table.alternateRowColor", Color(0x3A3D40))

    SwingUtilities.invokeLater {
        val frame = JFrame(title)
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE

        val cellsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(12, 16, 12, 16)
        }

        fun addCell(code: String) {
            val n = cellCounter.incrementAndGet()
            lateinit var wrapper: JPanel
            val cell = NotebookCell(code, n) {
                cellsPanel.remove(wrapper)
                cellsPanel.revalidate(); cellsPanel.repaint()
            }
            wrapper = JPanel(BorderLayout()).apply {
                alignmentX = Component.LEFT_ALIGNMENT
                maximumSize = Dimension(Int.MAX_VALUE, Short.MAX_VALUE.toInt())
                add(cell, BorderLayout.CENTER)
                border = BorderFactory.createEmptyBorder(0, 0, 10, 0)
                isOpaque = false
            }
            cellsPanel.add(wrapper)
            cellsPanel.revalidate(); cellsPanel.repaint()
        }

        addCell(TEMPLATE_TABLE)
        addCell(TEMPLATE_CHART)

        val header = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color(0x4A4A4A)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            )
            add(JLabel("KDF Notebook").apply {
                font = font.deriveFont(Font.BOLD, 16f)
                foreground = Color(0xBBBBBB)
            }, BorderLayout.WEST)
            add(JButton("＋  Add Cell").apply {
                putClientProperty("JButton.buttonType", "default")
                font = font.deriveFont(Font.BOLD, 13f)
                addActionListener { addCell(TEMPLATE_NEW) }
            }, BorderLayout.EAST)
        }

        frame.layout = BorderLayout()
        frame.add(header, BorderLayout.NORTH)
        frame.add(JScrollPane(cellsPanel).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBar.unitIncrement = 20
        }, BorderLayout.CENTER)
        frame.add(JLabel("  ▶ Table — last expression → DataFrame  |  ▶ Chart — use .save(outputPath)  |  First run compiles script (~3s)").apply {
            font = font.deriveFont(11f); foreground = Color(0x666666)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color(0x4A4A4A)),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
            )
        }, BorderLayout.SOUTH)

        frame.setSize(980, 780)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }
}

fun main() {
    showKandyNotebook("KDF Notebook")
}
