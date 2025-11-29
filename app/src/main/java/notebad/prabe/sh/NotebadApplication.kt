package notebad.prabe.sh

import android.app.Application
import notebad.prabe.sh.ui.viewmodel.ViewModelFactory

/**
 * Application class for Notebad.
 * Provides application-level dependencies.
 */
class NotebadApplication : Application() {

    /**
     * Lazy-initialized ViewModelFactory for creating ViewModels with dependencies.
     */
    val viewModelFactory: ViewModelFactory by lazy {
        ViewModelFactory(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NotebadApplication
            private set
    }
}
