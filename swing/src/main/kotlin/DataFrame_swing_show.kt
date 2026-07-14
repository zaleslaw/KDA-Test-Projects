import com.formdev.flatlaf.FlatDarkLaf
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.columnOf
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter

private fun setupLaf() {
    FlatDarkLaf.setup()
    UIManager.put("Table.alternateRowColor", Color(0x3A3D40))
    UIManager.put("Table.rowHeight", 26)
}

private fun buildDataFramePanel(df: DataFrame<*>): JPanel {
    val columns = df.columns()

    val model = object : AbstractTableModel() {
        override fun getRowCount() = df.rowsCount()
        override fun getColumnCount() = columns.size
        override fun getColumnName(column: Int) = columns[column].name()
        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? = columns[columnIndex][rowIndex]
    }

    val table = JTable(model).apply {
        fillsViewportHeight = true
        rowHeight = 26
        showHorizontalLines = false
        showVerticalLines = false
        intercellSpacing = java.awt.Dimension(0, 0)
        selectionBackground = Color(0x4B6EAF)
        selectionForeground = Color.WHITE
        font = Font("JetBrains Mono", Font.PLAIN, 13).let {
            if (it.family == "JetBrains Mono") it else Font(Font.MONOSPACED, Font.PLAIN, 13)
        }
    }

    val sorter = TableRowSorter(model)
    table.rowSorter = sorter

    // Type-aware renderer: numbers right-aligned, null dimmed, doubles 2dp
    val renderer = object : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            t: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, col: Int
        ): Component {
            val display = when (value) {
                null -> "—"
                is Double -> "%.2f".format(value)
                is Float  -> "%.2f".format(value)
                else      -> value
            }
            val c = super.getTableCellRendererComponent(t, display, isSelected, hasFocus, row, col)
            horizontalAlignment = if (value is Number) RIGHT else LEFT
            border = BorderFactory.createEmptyBorder(0, 8, 0, 8)
            if (!isSelected) foreground = if (value == null) Color(0x777777) else t.foreground
            return c
        }
    }
    for (i in 0 until model.columnCount) table.columnModel.getColumn(i).cellRenderer = renderer

    // Header: bold, with type hint
    val existingHeaderRenderer = table.tableHeader.defaultRenderer
    table.tableHeader.defaultRenderer = TableCellRenderer { t, value, isSelected, hasFocus, row, col ->
        val modelCol = t?.convertColumnIndexToModel(col) ?: col
        val colData = columns[modelCol]
        val typeHint = when (colData.values().firstOrNull()) {
            is Number  -> " #"
            is Boolean -> " ✓"
            else       -> ""
        }
        val c = existingHeaderRenderer.getTableCellRendererComponent(t, "$value$typeHint", isSelected, hasFocus, row, col)
        (c as? JLabel)?.apply {
            font = font.deriveFont(Font.BOLD)
            border = BorderFactory.createEmptyBorder(0, 8, 0, 8)
        }
        c
    }
    table.tableHeader.reorderingAllowed = true

    // Stats tooltip on column header hover
    table.tableHeader.addMouseMotionListener(object : MouseAdapter() {
        override fun mouseMoved(e: MouseEvent) {
            val col = table.columnModel.getColumnIndexAtX(e.x).takeIf { it >= 0 } ?: return
            val modelCol = table.convertColumnIndexToModel(col)
            val colData = columns[modelCol]
            val values = (0 until df.rowsCount()).mapNotNull { colData[it] }
            table.tableHeader.toolTipText = buildString {
                append("<html><b>${colData.name()}</b><br>")
                append("non-null: ${values.size} / ${df.rowsCount()}<br>")
                if (values.firstOrNull() is Number) {
                    val nums = values.map { (it as Number).toDouble() }
                    append("min: ${"%.2f".format(nums.min())}  |  ")
                    append("max: ${"%.2f".format(nums.max())}  |  ")
                    append("avg: ${"%.2f".format(nums.average())}")
                }
                append("</html>")
            }
        }
    })

    // Ctrl+C: copy selection as TSV
    val copyAction = object : AbstractAction() {
        override fun actionPerformed(e: java.awt.event.ActionEvent) {
            val rows = table.selectedRows
            val cols = (0 until model.columnCount).toList()
            val sb = StringBuilder()
            sb.appendLine(cols.joinToString("\t") { model.getColumnName(it) })
            for (row in rows) {
                val mr = table.convertRowIndexToModel(row)
                sb.appendLine(cols.joinToString("\t") { model.getValueAt(mr, it)?.toString() ?: "" })
            }
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(sb.toString()), null)
        }
    }
    table.getInputMap(JComponent.WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "copy")
    table.actionMap.put("copy", copyAction)

    // Filter bar
    val filterField = JTextField(22).apply {
        toolTipText = "Regex filter across all columns"
        putClientProperty("JTextField.placeholderText", "Filter rows…")
    }
    val clearBtn = JButton("✕").apply {
        isFocusable = false
        toolTipText = "Clear filter"
        putClientProperty("JButton.buttonType", "borderless")
        addActionListener { filterField.text = "" }
    }

    // Status bar
    val statusLabel = JLabel().apply {
        font = font.deriveFont(11f)
        foreground = Color(0x999999)
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color(0x4A4A4A)),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        )
    }

    fun updateStatus() {
        val sel = table.selectedRowCount
        statusLabel.text = "${table.rowCount} / ${df.rowsCount()} rows  ·  ${columns.size} cols" +
            (if (sel > 0) "  ·  $sel selected" else "")
    }
    updateStatus()

    val filterListener = object : DocumentListener {
        fun update() {
            val text = filterField.text
            if (text.isBlank()) sorter.rowFilter = null
            else runCatching { sorter.rowFilter = RowFilter.regexFilter("(?i)$text") }
            updateStatus()
        }
        override fun insertUpdate(e: DocumentEvent) = update()
        override fun removeUpdate(e: DocumentEvent) = update()
        override fun changedUpdate(e: DocumentEvent) = update()
    }
    filterField.document.addDocumentListener(filterListener)
    table.selectionModel.addListSelectionListener { updateStatus() }

    val filterBar = JPanel(BorderLayout(4, 0)).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color(0x4A4A4A)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        )
        add(JLabel("Filter ").apply { foreground = Color(0x888888); font = font.deriveFont(12f) }, BorderLayout.WEST)
        add(filterField, BorderLayout.CENTER)
        add(clearBtn, BorderLayout.EAST)
    }

    return JPanel(BorderLayout()).apply {
        add(filterBar, BorderLayout.NORTH)
        add(JScrollPane(table).apply {
            border = BorderFactory.createEmptyBorder()
        }, BorderLayout.CENTER)
        add(statusLabel, BorderLayout.SOUTH)
    }
}

fun DataFrame<*>.swingShow(title: String = "DataFrame") {
    setupLaf()
    SwingUtilities.invokeLater {
        JFrame(title).apply {
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            add(buildDataFramePanel(this@swingShow))
            setSize(900, 500)
            setLocationRelativeTo(null)
            isVisible = true
        }
    }
}

fun Map<String, DataFrame<*>>.swingShow(title: String = "DataFrames") {
    setupLaf()
    SwingUtilities.invokeLater {
        JFrame(title).apply {
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            val tabs = JTabbedPane().apply {
                this@swingShow.forEach { (name, df) ->
                    addTab(name, buildDataFramePanel(df))
                    val idx = tabCount - 1
                    setTabComponentAt(idx, JLabel("  $name  [${df.rowsCount()}×${df.columnsCount()}]  ").apply {
                        font = font.deriveFont(12f)
                    })
                }
            }
            add(tabs)
            setSize(1100, 540)
            setLocationRelativeTo(null)
            isVisible = true
        }
    }
}

fun List<Pair<String, DataFrame<*>>>.swingShow(title: String = "DataFrames") =
    toMap(LinkedHashMap()).swingShow(title)

fun main() {
    val employees = dataFrameOf(
        "name"       to columnOf("Alice", "Bob", "Charlie", "Diana", "Eve"),
        "age"        to columnOf(30, 25, 35, 28, 32),
        "salary"     to columnOf(75000.0, 60000.0, 90000.0, 80000.0, 70000.0),
        "department" to columnOf("Engineering", "Marketing", "Engineering", "HR", "Finance")
    )

    val departments = dataFrameOf(
        "department" to columnOf("Engineering", "Marketing", "HR", "Finance"),
        "headcount"  to columnOf(2, 1, 1, 1),
        "budget"     to columnOf(165000.0, 60000.0, 80000.0, 70000.0)
    )

    // 1. Single DataFrame
    employees.swingShow("Employee Data")

    // 2. Multiple DataFrames in tabs (Map)
    mapOf("Employees" to employees, "Departments" to departments).swingShow("Company Data")

    // 3. Multiple DataFrames in tabs (List, preserves order)
    listOf("Employees" to employees, "Departments" to departments).swingShow("Company Data (list)")
}
