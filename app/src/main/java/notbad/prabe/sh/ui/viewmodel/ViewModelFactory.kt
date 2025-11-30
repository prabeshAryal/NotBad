package notbad.prabe.sh.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import notbad.prabe.sh.core.io.LargeFileRepository

/**
 * Factory for creating ViewModels with dependencies.
 */
class ViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    private val repository: LargeFileRepository by lazy {
        LargeFileRepository(context.applicationContext)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(FileViewerViewModel::class.java) -> {
                FileViewerViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
