import java.awt.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import javax.imageio.ImageIO
import javax.script.ScriptEngineManager
import javax.swing.*

private val DEFAULT_CODE = """
import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.layers.bars

// `outputPath` is injected — save your plot to it
val df = dataFrameOf(
    "month"   to columnOf("Jan", "Feb", "Mar", "Apr", "May", "Jun"),
    "revenue" to columnOf(42.0, 38.5, 51.0, 47.3, 62.0, 58.9)
)

df.plot {
    bars {
        x("month")
        y("revenue")
    }
}.save(outputPath)
""".trimIndent()

fun showKandyPlayground(title: String = "Kandy Playground") {
    SwingUtilities.invokeLater {
        val frame = JFrame(title)
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE

        // ── Code editor ──────────────────────────────────────────────────
        val codeArea = JTextArea(DEFAULT_CODE).apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, 13)
            tabSize = 4
        }
        val editorPanel = JPanel(BorderLayout()).apply {
            add(JLabel("  Kotlin / Kandy script  (use outputPath to save your plot)"), BorderLayout.NORTH)
            add(JScrollPane(codeArea), BorderLayout.CENTER)
        }

        // ── Chart display ────────────────────────────────────────────────
        val chartLabel = JLabel("Press ▶ Execute to render", SwingConstants.CENTER)

        // ── Console ──────────────────────────────────────────────────────
        val consoleArea = JTextArea().apply {
            isEditable = false
            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            foreground = Color(0x00CC66)
            background = Color(0x1E1E1E)
        }
        val consolePanel = JPanel(BorderLayout()).apply {
            add(JLabel("  Console output"), BorderLayout.NORTH)
            add(JScrollPane(consoleArea), BorderLayout.CENTER)
        }

        // ── Execute ──────────────────────────────────────────────────────
        fun execute() {
            consoleArea.text = "Compiling and running…\n"
            chartLabel.icon = null
            chartLabel.text = "Rendering…"

            Thread {
                val outputFile = Files.createTempFile("kandy_play_", ".png").toFile()
                val log = StringBuilder()

                val origOut = System.out
                val capture = ByteArrayOutputStream()
                System.setOut(PrintStream(capture))

                val result = runCatching {
                    val engine = ScriptEngineManager().getEngineByExtension("kts")
                        ?: error("Kotlin scripting engine not found – check kotlin-scripting-jsr223 dependency")
                    engine.put("outputPath", outputFile.absolutePath)
                    engine.eval(codeArea.text)
                }

                System.setOut(origOut)
                val stdout = capture.toString()
                if (stdout.isNotBlank()) log.appendLine(stdout)

                result
                    .onSuccess { log.appendLine("✓ OK") }
                    .onFailure { e ->
                        log.appendLine("✗ ${e::class.simpleName}: ${e.message}")
                        e.cause?.let { log.appendLine("  Caused by: ${it.message}") }
                    }

                SwingUtilities.invokeLater {
                    consoleArea.text = log.toString()
                    if (outputFile.exists() && outputFile.length() > 0) {
                        val img = ImageIO.read(outputFile)
                        val w = chartLabel.width.coerceAtLeast(500)
                        val h = chartLabel.height.coerceAtLeast(350)
                        chartLabel.icon = ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH))
                        chartLabel.text = null
                    } else {
                        chartLabel.text = if (result.isSuccess)
                            "Script ran but produced no chart — did you call .save(outputPath)?"
                        else
                            "Error — see console below"
                    }
                }
                outputFile.deleteOnExit()
            }.start()
        }

        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton("▶  Execute").apply {
                font = font.deriveFont(Font.BOLD, 13f)
                addActionListener { execute() }
            })
            add(JButton("Reset").apply {
                addActionListener { codeArea.text = DEFAULT_CODE }
            })
            add(JButton("Clear console").apply {
                addActionListener { consoleArea.text = "" }
            })
        }

        // ── Layout ───────────────────────────────────────────────────────
        val rightSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT,
            JScrollPane(chartLabel), consolePanel).apply {
            resizeWeight = 0.75
            dividerLocation = 370
        }
        val mainSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanel, rightSplit).apply {
            dividerLocation = 480
            resizeWeight = 0.43
        }

        frame.layout = BorderLayout()
        frame.add(toolbar, BorderLayout.NORTH)
        frame.add(mainSplit, BorderLayout.CENTER)
        frame.setSize(1200, 620)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }
}

fun main() {
    showKandyPlayground("Kandy Script Playground")
}
