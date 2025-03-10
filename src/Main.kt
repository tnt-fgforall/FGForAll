import javax.swing.SwingUtilities

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater { FileOperationUI().isVisible = true }
    }
}
