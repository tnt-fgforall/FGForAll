import com.google.gson.Gson
import java.io.File

class GameLocatorService {
    private val specificSteamPathC = "C:/Program Files (x86)/Steam/steamapps/common"
    private val steamBasePath = "SteamLibrary/steamapps/common"
    private val ignorePaths = listOf("Steamworks Shared")
    private val epicGamesDatabasePath = File(System.getenv("PROGRAMDATA"), "Epic/EpicGamesLauncher/Data/Manifests")

    data class Game(val name: String, val mainLocation: String, val dllLocation: String?)

    private val gson = Gson()

    fun locateGames(): List<Game> {
        val steamGames = mutableListOf<Game>()

        // Handle C drive specifically
        val steamPathC = File(specificSteamPathC)
        if (steamPathC.exists()) {
            searchGames(steamPathC, steamGames)
        }

        // Handle other drives
        getAvailableDrives().forEach { drive ->
            val steamPath = File("$drive/$steamBasePath")
            if (steamPath.exists()) {
                searchGames(steamPath, steamGames)
            }
        }

        val epicGames = readEpicGamesDatabase(epicGamesDatabasePath)

        return (steamGames + epicGames).filter { game -> game.dllLocation != null }
    }

    private fun searchGames(path: File, games: MutableList<Game>) {
        path.listFiles()?.forEach { dir ->
            if (dir.isDirectory && !ignorePaths.contains(dir.name)) {
                val gameName = dir.name
                val dllLocation = findDllFile(dir)
                games.add(Game(gameName, dir.absolutePath, dllLocation))
            }
        }
    }

    private fun findDllFile(dir: File): String? {
        dir.walkTopDown().forEach { file ->
            if (file.isFile && file.name.equals("nvngx_dlss.dll", ignoreCase = true)) {
                return file.parentFile.absolutePath
            }
        }
        return null
    }

    private fun readEpicGamesDatabase(databasePath: File): List<Game> {
        val games = mutableListOf<Game>()
        if (databasePath.exists() && databasePath.isDirectory) {
            databasePath.listFiles()?.forEach { file ->
                if (!file.isDirectory) {
                    val gameData = file.readText()
                    val manifest = gson.fromJson(gameData, EpicGameManifest::class.java)
                    val dllLocation = findDllFile(File(manifest.InstallLocation))
                    games.add(Game(manifest.DisplayName, manifest.InstallLocation, dllLocation))
                }
            }
        }
        return games
    }

    /**
     * Get all available drives on the system
     */
    private fun getAvailableDrives(): List<String> {
        return try {
            // Get all root directories from the file system
            File.listRoots().mapNotNull { drive ->
                // Convert to standard path format and ensure it's a valid drive
                val drivePath = drive.absolutePath.replace("\\", "/").trimEnd('/')

                // Check if it's a valid drive and is readable
                if (drive.exists() && drive.canRead() && drivePath.endsWith(":")) {
                    drivePath
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            // Fallback to common drives if there's an error
            listOf("C:", "D:", "E:", "F:")
        }
    }

    data class EpicGameManifest(
        val DisplayName: String,
        val InstallLocation: String
    )
}
