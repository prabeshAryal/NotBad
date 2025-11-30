package notbad.prabe.sh

import android.app.Application
import notbad.prabe.sh.ui.viewmodel.ViewModelFactory

/**
 * Application class for Notbad.
 * Provides application-level dependencies.
 */
class NotbadApplication : Application() {

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
        lateinit var instance: NotbadApplication
            private set
    }
}
