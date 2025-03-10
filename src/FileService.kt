import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class FileService {
    fun performFileOperations(gameFolderPath: String, option: String, operation: Operation): String {
        if (gameFolderPath == "" || !File(gameFolderPath).exists())
            return "Game folder is not valid!"
        val bakFolderPath = "$gameFolderPath/bak"
        val bakFolder = File(bakFolderPath)
        val subFolderPath = "$DLSS_FG_PATH/$option"
        val subFolder = File(subFolderPath)

        subFolder.listFiles() ?: return "Please add the dlss-fg folder!"

        return if (operation == Operation.ROLLBACK) {
            subFolder.listFiles()?.filterNot { READMES.contains(it.name) }?.forEach { file ->
                val destinationFilePath = Paths.get("$gameFolderPath/${file.name}")
                val bakFilePath = Paths.get("$bakFolderPath/${file.name}")

                if (file.name == FSR_FG_FILE) {
                    Files.deleteIfExists(destinationFilePath)
                }
                if (Files.exists(bakFilePath)) {
                    Files.move(bakFilePath, destinationFilePath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
            bakFolder.listFiles().takeIf { it.isNullOrEmpty() }.let { bakFolder.deleteRecursively() }

            "Files have been reverted to the original location, and the bak folder has been removed."
        } else if (operation == Operation.INSTALL) {
            if (bakFolder.exists())
                return "Another mod has already been installed, try rollback first."

            bakFolder.mkdirs()
            subFolder.listFiles()?.filterNot { READMES.contains(it.name) }?.forEach { file ->
                val destinationFilePath = Paths.get("$gameFolderPath/${file.name}")
                val bakFilePath = Paths.get("$bakFolderPath/${file.name}")
                if (Files.exists(destinationFilePath)) {
                    Files.move(destinationFilePath, bakFilePath, StandardCopyOption.REPLACE_EXISTING)
                }

                Files.copy(file.toPath(), destinationFilePath, StandardCopyOption.REPLACE_EXISTING)
            }
            "Files have been copied from $option to the game folder."
        } else
            "Unknown Operation"
    }

    enum class Operation {
        INSTALL, ROLLBACK
    }

    companion object {
        const val FSR_FG_FILE = "dlssg_to_fsr3_amd_is_better.dll"
        const val DLSS_FG_PATH = "dlss-fg"
        val READMES = listOf("READ ME.txt", "README.txt")
    }
}
