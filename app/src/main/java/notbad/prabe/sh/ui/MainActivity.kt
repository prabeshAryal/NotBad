package notbad.prabe.sh.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import notbad.prabe.sh.NotbadApplication
import notbad.prabe.sh.ui.screens.FileViewerScreen
import notbad.prabe.sh.ui.theme.NotbadTheme
import notbad.prabe.sh.ui.viewmodel.FileViewerViewModel

/**
 * Main Activity for Notbad.
 *
 * Handles:
 * - Incoming intents for opening files (VIEW, EDIT, SEND actions)
 * - File picker for opening files from within the app
 * - URI permission management for Scoped Storage
 */
class MainActivity : ComponentActivity() {

    private val viewModel: FileViewerViewModel by viewModels {
        (application as NotbadApplication).viewModelFactory
    }

    /**
     * File picker launcher using the Storage Access Framework.
     * Supports all file types.
     */
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleUri(it, persistPermissions = true) }
    }
    
    /**
     * File creator launcher using the Storage Access Framework.
     * Allows creating new files.
     */
    private val fileCreatorLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let { handleUri(it, persistPermissions = true) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming intent
        handleIntent(intent)

        setContent {
            NotbadTheme {
                FileViewerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onOpenFile = { openFilePicker() },
                    onCreateFile = { createNewFile() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Handles incoming intents to extract and open the file URI.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW, Intent.ACTION_EDIT -> {
                intent.data
            }
            Intent.ACTION_SEND -> {
                // Handle files shared to this app
                if (intent.type != null) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                } else null
            }
            else -> null
        }

        uri?.let { handleUri(it, persistPermissions = false) }
    }

    /**
     * Processes a URI and opens the file.
     *
     * @param uri The file URI to open
     * @param persistPermissions Whether to persist URI permissions (for files opened via picker)
     */
    private fun handleUri(uri: Uri, persistPermissions: Boolean) {
        try {
            // Take persistable URI permission if possible
            if (persistPermissions) {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                try {
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: SecurityException) {
                    // Permission might not be persistable, that's okay
                    try {
                        // Try read-only permission
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e2: SecurityException) {
                        // Still okay, we'll use the temporary permission
                    }
                }
            }

            // Verify we can access the URI
            contentResolver.openInputStream(uri)?.close()
                ?: throw IllegalStateException("Cannot access file")

            // Open the file in the ViewModel
            viewModel.openFile(uri)

        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "Permission denied: Cannot access this file",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error opening file: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Opens the system file picker.
     */
    private fun openFilePicker() {
        try {
            filePickerLauncher.launch(arrayOf("*/*"))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error opening file picker: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Creates a new file.
     */
    private fun createNewFile() {
        try {
            fileCreatorLauncher.launch("untitled.txt")
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error creating file: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
