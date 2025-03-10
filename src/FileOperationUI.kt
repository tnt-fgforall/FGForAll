import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.FlowLayout.LEFT
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import javax.swing.*

class FileOperationUI : JFrame() {
    private val gamesComboBox: JComboBox<String> = JComboBox()
    private val gameFolderPathField: JTextField = JTextField()
    private val optionComboBox: JComboBox<String> =
        JComboBox(arrayOf("dll_version", "dll_winhttp", "dll_dbghelp", "plugin_asi_loader", "plugin_red4ext"))
    private val installOperationButton: JButton = JButton("Install")
    private val rollbackOperationButton: JButton = JButton("Rollback")
    private val resultLabel: JLabel = JLabel()
    private val gameLocator = GameLocatorService()
    private val games = mutableListOf<GameLocatorService.Game>()

    init {
        title = "DLSS FG"
        setSize(600, 400)
        defaultCloseOperation = EXIT_ON_CLOSE
        setLocationRelativeTo(null)

        // Load games using GameLocatorService
        loadGames()

        gamesComboBox.addActionListener {
            val selectedIndex = gamesComboBox.selectedIndex
            if (selectedIndex > 0) { // Skip the first "Select a game" item
                val selectedGame = games[selectedIndex - 1]
                gameFolderPathField.text = selectedGame.dllLocation
            }
        }

        installOperationButton.addActionListener {
            val gameFolderPath = gameFolderPathField.text
            val option = optionComboBox.selectedItem as String
            val fileService = FileService()
            val result = fileService.performFileOperations(gameFolderPath, option, FileService.Operation.INSTALL)
            resultLabel.text = result
        }

        rollbackOperationButton.addActionListener {
            val gameFolderPath = gameFolderPathField.text
            val option = optionComboBox.selectedItem as String
            val fileService = FileService()
            val result = fileService.performFileOperations(gameFolderPath, option, FileService.Operation.ROLLBACK)
            resultLabel.text = result
        }

        gameFolderPathField.dropTarget = DropTarget().apply {
            addDropTargetListener(object : DropTargetAdapter() {
                override fun drop(dtde: DropTargetDropEvent) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY)
                    val transferable = dtde.transferable
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                        if (files.isNotEmpty()) {
                            val gameFolderPath = files[0].parent
                            gameFolderPathField.text = gameFolderPath
                            // Reset the games combo box selection
                            gamesComboBox.selectedIndex = 0
                        }
                    }
                }
            })
        }

        // Set up the layout with panels for better organization
        layout = BorderLayout()

        // Game selection panel
        val gameSelectionPanel = JPanel()
        gameSelectionPanel.layout = BoxLayout(gameSelectionPanel, BoxLayout.Y_AXIS)
        gameSelectionPanel.border = BorderFactory.createTitledBorder("Game Selection")

        val gamesLabel = JLabel("Select a game:")
        gamesLabel.alignmentX = LEFT_ALIGNMENT
        gameSelectionPanel.add(gamesLabel)

        gamesComboBox.alignmentX = LEFT_ALIGNMENT
        gameSelectionPanel.add(gamesComboBox)

        val pathLabel = JLabel("Or drag and drop any file from the game folder here or enter the path:", null, LEFT)
        pathLabel.alignmentX = LEFT_ALIGNMENT
        gameSelectionPanel.add(Box.createVerticalStrut(10))
        gameSelectionPanel.add(pathLabel)

        gameFolderPathField.alignmentX = LEFT_ALIGNMENT
        gameSelectionPanel.add(gameFolderPathField)

        // Options panel
        val optionsPanel = JPanel()
        optionsPanel.layout = BoxLayout(optionsPanel, BoxLayout.Y_AXIS)
        optionsPanel.border = BorderFactory.createTitledBorder("Options")

        val optionLabel = JLabel("Select mod option:")
        optionLabel.alignmentX = LEFT_ALIGNMENT
        optionsPanel.add(optionLabel)

        optionComboBox.alignmentX = LEFT_ALIGNMENT
        optionsPanel.add(optionComboBox)

        // Button panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        buttonPanel.add(installOperationButton)
        buttonPanel.add(rollbackOperationButton)

        // Result panel
        val resultPanel = JPanel(BorderLayout())
        resultPanel.border = BorderFactory.createTitledBorder("Result")
        resultPanel.add(resultLabel, BorderLayout.CENTER)

        // Add all panels to the main layout
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.add(gameSelectionPanel)
        mainPanel.add(optionsPanel)

        add(mainPanel, BorderLayout.NORTH)
        add(buttonPanel, BorderLayout.CENTER)
        add(resultPanel, BorderLayout.SOUTH)
    }

    private fun loadGames() {
        // Use SwingWorker to load games in background
        object : SwingWorker<List<GameLocatorService.Game>, Void>() {
            override fun doInBackground(): List<GameLocatorService.Game> {
                return gameLocator.locateGames()
            }

            override fun done() {
                try {
                    // Clear the loading message
                    gamesComboBox.removeAllItems()

                    // Add default selection
                    gamesComboBox.addItem("Select a game")

                    // Get the game list
                    games.clear()
                    games.addAll(get())

                    // Add games to the combo box
                    games.forEach { game ->
                        gamesComboBox.addItem(game.name)
                    }

                    // Set default selection
                    gamesComboBox.selectedIndex = 0
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(
                        this@FileOperationUI,
                        "Error loading games: ${e.message}",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }.execute()
    }
}
