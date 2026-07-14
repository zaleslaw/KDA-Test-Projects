import com.formdev.flatlaf.FlatDarkLaf
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import java.awt.*
import java.awt.print.PageFormat
import java.awt.print.Printable
import java.awt.print.PrinterJob
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import javax.script.ScriptEngineManager
import javax.swing.*
import javax.swing.table.AbstractTableModel
import kotlin.math.ceil

private val cellCounterV2 = AtomicInteger(0)

// ── Cell output state ────────────────────────────────────────────────────────

private sealed class CellOutput {
    object None : CellOutput()
    data class TableOutput(val df: DataFrame<*>) : CellOutput()
    data class ChartOutput(val file: java.io.File) : CellOutput()
    data class ErrorOutput(val msg: String) : CellOutput()
}

// ── Templates ────────────────────────────────────────────────────────────────

private val NB2_TEMPLATE_TABLE = """
import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf

dataFrameOf(
    "name"   to columnOf("Alice", "Bob", "Charlie", "Diana"),
    "age"    to columnOf(30, 25, 35, 28),
    "salary" to columnOf(75000.0, 60000.0, 90000.0, 80000.0)
)
""".trimIndent()

private val NB2_TEMPLATE_CHART = """
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

private val NB2_TEMPLATE_NEW = """
import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf

dataFrameOf(
    "x" to columnOf(1, 2, 3, 4, 5),
    "y" to columnOf(10.0, 25.0, 18.0, 30.0, 22.0)
)
""".trimIndent()

// ── Notebook cell ────────────────────────────────────────────────────────────

private class NotebookCellV2(
    initialCode: String,
    cellNum: Int,
    onRemove: (NotebookCellV2) -> Unit
) : JPanel(BorderLayout()) {

    val codeArea = JTextArea(initialCode).apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 13); tabSize = 4; rows = 6
    }
    var currentOutput: CellOutput = CellOutput.None
    private val outputPanel = JPanel(BorderLayout()).apply { isVisible = false }

    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, Color(0x4B6EAF)),
            BorderFactory.createEmptyBorder(8, 8, 6, 8)
        )
        val inLabel = JLabel("  In [$cellNum]").apply {
            font = font.deriveFont(Font.BOLD, 12f); foreground = Color(0x6897BB)
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
            addActionListener { onRemove(this@NotebookCellV2) }
        }
        val toolbar = JPanel(BorderLayout()).apply {
            add(JPanel(FlowLayout(FlowLayout.LEFT, 6, 2)).apply {
                add(inLabel); add(btnTable); add(btnChart)
            }, BorderLayout.WEST)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 2)).apply { add(btnRemove) }, BorderLayout.EAST)
        }
        outputPanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color(0x4A4A4A)),
            BorderFactory.createEmptyBorder(8, 0, 4, 0)
        )
        add(toolbar, BorderLayout.NORTH)
        add(JScrollPane(codeArea).apply {
            border = BorderFactory.createLineBorder(Color(0x4A4A4A)); minimumSize = Dimension(200, 80)
        }, BorderLayout.CENTER)
        add(outputPanel, BorderLayout.SOUTH)
    }

    fun cleanup() { (currentOutput as? CellOutput.ChartOutput)?.file?.delete() }

    private fun setOutput(component: JComponent?) {
        outputPanel.removeAll(); outputPanel.isVisible = component != null
        if (component != null) outputPanel.add(component, BorderLayout.CENTER)
        revalidate(); repaint()
    }

    private fun showRunning() {
        outputPanel.removeAll()
        outputPanel.add(JProgressBar().apply {
            isIndeterminate = true; isStringPainted = true; string = "Running…"
        }, BorderLayout.NORTH)
        outputPanel.isVisible = true; revalidate(); repaint()
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
        return JPanel(BorderLayout()).apply {
            add(JScrollPane(table).apply {
                preferredSize = Dimension(800, displayRows.coerceAtMost(8) * 26 + table.tableHeader.preferredSize.height + 4)
                border = BorderFactory.createLineBorder(Color(0x4A4A4A))
            }, BorderLayout.CENTER)
            if (df.rowsCount() > 50) add(JLabel("  Showing 50 of ${df.rowsCount()} rows").apply {
                font = font.deriveFont(11f); foreground = Color(0x888888)
            }, BorderLayout.SOUTH)
        }
    }

    private fun buildError(msg: String): JComponent =
        JTextArea("⚠  $msg").apply {
            isEditable = false; font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            foreground = Color(0xFF7777); background = Color(0x3C1010)
            border = BorderFactory.createEmptyBorder(8, 10, 8, 10); lineWrap = true; wrapStyleWord = true
        }

    fun executeAsTable() {
        (currentOutput as? CellOutput.ChartOutput)?.file?.delete()
        showRunning()
        val origOut = System.out
        Thread {
            System.setOut(PrintStream(ByteArrayOutputStream()))
            val result = runCatching {
                val e = ScriptEngineManager().getEngineByExtension("kts")
                    ?: error("Kotlin scripting engine not found")
                e.eval(codeArea.text)
            }
            System.setOut(origOut)
            SwingUtilities.invokeLater {
                result
                    .onSuccess { v ->
                        currentOutput = if (v is DataFrame<*>) CellOutput.TableOutput(v) else CellOutput.None
                        setOutput(if (v is DataFrame<*>) buildTable(v) else JLabel("  $v"))
                    }
                    .onFailure { e ->
                        currentOutput = CellOutput.ErrorOutput("${e::class.simpleName}: ${e.message}")
                        setOutput(buildError("${e::class.simpleName}: ${e.message}"))
                    }
            }
        }.start()
    }

    fun executeAsChart() {
        (currentOutput as? CellOutput.ChartOutput)?.file?.delete()
        showRunning()
        val tempFile = Files.createTempFile("kandy_nb_", ".png").toFile()
        val origOut = System.out
        Thread {
            System.setOut(PrintStream(ByteArrayOutputStream()))
            val result = runCatching {
                val e = ScriptEngineManager().getEngineByExtension("kts")
                    ?: error("Kotlin scripting engine not found")
                e.put("outputPath", tempFile.absolutePath)
                e.eval(codeArea.text)
            }
            System.setOut(origOut)
            SwingUtilities.invokeLater {
                if (tempFile.exists() && tempFile.length() > 0) {
                    currentOutput = CellOutput.ChartOutput(tempFile)
                    val img = ImageIO.read(tempFile)
                    val w = img.width.coerceAtMost(860)
                    val scaled = img.getScaledInstance(w, (img.height * w.toDouble() / img.width).toInt(), Image.SCALE_SMOOTH)
                    setOutput(JLabel(ImageIcon(scaled)).apply { border = BorderFactory.createEmptyBorder(4, 0, 4, 0) })
                } else {
                    val msg = result.exceptionOrNull()?.let { "${it::class.simpleName}: ${it.message}" }
                        ?: "No chart — did you call .save(outputPath)?"
                    currentOutput = CellOutput.ErrorOutput(msg)
                    setOutput(buildError(msg))
                }
            }
        }.start()
    }
}

// ── Export: HTML ─────────────────────────────────────────────────────────────

private fun String.esc() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private fun fmtVal(v: Any?) = when (v) {
    null -> "—"; is Double, is Float -> "%.2f".format((v as Number).toDouble()); else -> v.toString()
}

private fun exportNotebookToHtml(cells: List<NotebookCellV2>, parentFrame: JFrame) {
    val html = buildString {
        append("""<!DOCTYPE html><html><head><meta charset="UTF-8">
<style>
body{background:#2B2B2B;color:#A9B7C6;font-family:'JetBrains Mono',monospace;max-width:960px;margin:0 auto;padding:24px}
h1{color:#BBBBBB;font-size:20px;margin-bottom:20px}
.cell{border-left:3px solid #4B6EAF;padding:10px 14px;margin-bottom:18px;background:#313335;border-radius:0 6px 6px 0}
.cell-num{color:#6897BB;font-weight:bold;font-size:11px;margin-bottom:6px}
pre{background:#2B2B2B;padding:10px 12px;border-radius:4px;font-size:12px;margin:0;overflow-x:auto;white-space:pre-wrap;word-break:break-all}
.output{margin-top:12px;border-top:1px solid #4A4A4A;padding-top:12px}
table{border-collapse:collapse;font-size:12px;width:100%}
th{background:#3D3F41;padding:6px 12px;text-align:left;font-weight:bold;color:#CC7832}
td{padding:5px 12px;border-bottom:1px solid #3A3A3A}
tr:nth-child(even) td{background:#3A3D40}
.error{color:#FF7777;background:#3C1010;padding:8px 10px;border-radius:4px;font-size:12px}
img{max-width:100%;border-radius:4px}
</style></head><body><h1>KDF Notebook</h1>""")

        cells.forEachIndexed { idx, cell ->
            append("""<div class="cell"><div class="cell-num">In [${idx + 1}]</div>""")
            append("<pre>${cell.codeArea.text.esc()}</pre>")
            when (val out = cell.currentOutput) {
                is CellOutput.TableOutput -> {
                    val df = out.df
                    append("""<div class="output"><table><tr>""")
                    df.columns().forEach { append("<th>${it.name().esc()}</th>") }
                    append("</tr>")
                    for (r in 0 until df.rowsCount().coerceAtMost(200)) {
                        append("<tr>")
                        df.columns().forEach { col -> append("<td>${fmtVal(col[r]).esc()}</td>") }
                        append("</tr>")
                    }
                    if (df.rowsCount() > 200)
                        append("""<tr><td colspan="${df.columnsCount()}" style="color:#888">… ${df.rowsCount() - 200} more rows</td></tr>""")
                    append("</table></div>")
                }
                is CellOutput.ChartOutput -> {
                    val b64 = Base64.getEncoder().encodeToString(out.file.readBytes())
                    append("""<div class="output"><img src="data:image/png;base64,$b64"/></div>""")
                }
                is CellOutput.ErrorOutput ->
                    append("""<div class="output"><div class="error">⚠ ${out.msg.esc()}</div></div>""")
                CellOutput.None -> Unit
            }
            append("</div>")
        }
        append("</body></html>")
    }

    val file = Files.createTempFile("kdf_notebook_", ".html").toFile()
    file.writeText(html)
    runCatching { java.awt.Desktop.getDesktop().browse(file.toURI()) }
        .onFailure { JOptionPane.showMessageDialog(parentFrame, "Saved to: ${file.absolutePath}") }
}

// ── Export: PDF via PrinterJob ───────────────────────────────────────────────

private fun exportNotebookToPdf(cells: List<NotebookCellV2>, parentFrame: JFrame) {
    val images = cells.map { cell ->
        val w = cell.width.takeIf { it > 10 } ?: 900
        val h = cell.height.takeIf { it > 10 } ?: cell.preferredSize.height.coerceAtLeast(50)
        val img = java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = Color(0x313335); g.fillRect(0, 0, w, h)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        cell.printAll(g); g.dispose()
        img
    }

    val job = PrinterJob.getPrinterJob()
    job.setPrintable(Printable { graphics, pageFormat, pageIndex ->
        val g2 = graphics as Graphics2D
        g2.translate(pageFormat.imageableX, pageFormat.imageableY)
        val pw = pageFormat.imageableWidth
        val ph = pageFormat.imageableHeight
        val scale = pw / (images.maxOfOrNull { it.width }?.toDouble() ?: pw)
        val gap = 12.0
        val totalH = images.sumOf { it.height * scale + gap }
        val totalPages = ceil(totalH / ph).toInt().coerceAtLeast(1)

        if (pageIndex >= totalPages) return@Printable Printable.NO_SUCH_PAGE

        val pageStartY = pageIndex * ph
        var y = 0.0
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        for (img in images) {
            val ih = img.height * scale
            if (y + ih > pageStartY && y < pageStartY + ph) {
                val srcY0 = ((pageStartY - y).coerceAtLeast(0.0) / scale).toInt()
                val dstY0 = (y - pageStartY).coerceAtLeast(0.0).toInt()
                val dstY1 = dstY0 + ((img.height - srcY0) * scale).toInt()
                g2.drawImage(img, 0, dstY0, (img.width * scale).toInt(), dstY1, 0, srcY0, img.width, img.height, null)
            }
            y += ih + gap
        }
        Printable.PAGE_EXISTS
    })

    if (job.printDialog()) {
        runCatching { job.print() }
            .onFailure { JOptionPane.showMessageDialog(parentFrame, "Print failed: ${it.message}") }
    }
}

// ── Notebook window ──────────────────────────────────────────────────────────

fun showKandyNotebookV2(title: String = "KDF Notebook") {
    FlatDarkLaf.setup()
    UIManager.put("Table.alternateRowColor", Color(0x3A3D40))

    SwingUtilities.invokeLater {
        val frame = JFrame(title)
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE

        val cells = mutableListOf<NotebookCellV2>()
        val cellsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(12, 16, 12, 16)
        }

        fun addCell(code: String) {
            val n = cellCounterV2.incrementAndGet()
            lateinit var wrapper: JPanel
            val cell = NotebookCellV2(code, n) { c ->
                cells.remove(c); c.cleanup()
                cellsPanel.remove(wrapper); cellsPanel.revalidate(); cellsPanel.repaint()
            }
            cells.add(cell)
            wrapper = JPanel(BorderLayout()).apply {
                alignmentX = Component.LEFT_ALIGNMENT
                maximumSize = Dimension(Int.MAX_VALUE, Short.MAX_VALUE.toInt())
                add(cell, BorderLayout.CENTER)
                border = BorderFactory.createEmptyBorder(0, 0, 10, 0); isOpaque = false
            }
            cellsPanel.add(wrapper); cellsPanel.revalidate(); cellsPanel.repaint()
        }

        addCell(NB2_TEMPLATE_TABLE)
        addCell(NB2_TEMPLATE_CHART)

        val header = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color(0x4A4A4A)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            )
            add(JLabel("KDF Notebook").apply {
                font = font.deriveFont(Font.BOLD, 16f); foreground = Color(0xBBBBBB)
            }, BorderLayout.WEST)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0)).apply {
                add(JButton("＋  Add Cell").apply {
                    putClientProperty("JButton.buttonType", "default")
                    font = font.deriveFont(Font.BOLD, 13f)
                    addActionListener { addCell(NB2_TEMPLATE_NEW) }
                })
                add(JButton("Export HTML").apply {
                    toolTipText = "Export all cells to a self-contained HTML file and open in browser"
                    addActionListener { exportNotebookToHtml(cells, frame) }
                })
                add(JButton("Export PDF").apply {
                    toolTipText = "Print notebook to PDF via system print dialog"
                    addActionListener { exportNotebookToPdf(cells, frame) }
                })
            }, BorderLayout.EAST)
        }

        frame.layout = BorderLayout()
        frame.add(header, BorderLayout.NORTH)
        frame.add(JScrollPane(cellsPanel).apply {
            border = BorderFactory.createEmptyBorder(); verticalScrollBar.unitIncrement = 20
        }, BorderLayout.CENTER)
        frame.add(JLabel("  ▶ Table — last expr → DataFrame  |  ▶ Chart — .save(outputPath)  |  First run compiles script (~3s)").apply {
            font = font.deriveFont(11f); foreground = Color(0x666666)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color(0x4A4A4A)),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
            )
        }, BorderLayout.SOUTH)

        frame.setSize(980, 780); frame.setLocationRelativeTo(null); frame.isVisible = true
    }
}

fun main() {
    showKandyNotebookV2("KDF Notebook v2 — with Export")
}
