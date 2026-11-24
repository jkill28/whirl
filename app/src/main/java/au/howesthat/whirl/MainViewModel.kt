package au.howesthat.whirl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.howesthat.whirl.core.data.PendingWatch
import au.howesthat.whirl.core.data.Settings
import au.howesthat.whirl.core.data.SettingsRepository
import au.howesthat.whirl.core.data.ThreadPageRepository
import au.howesthat.whirl.core.data.WatchedRepository
import au.howesthat.whirl.core.network.WhirlpoolApi
import au.howesthat.whirl.navigation.DeepLinkTarget
import au.howesthat.whirl.navigation.resolveDeepLink
import au.howesthat.whirl.navigation.routeForLink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainState(val settings: Settings, val setupDone: Boolean)

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
    api: WhirlpoolApi,
    private val watched: WatchedRepository,
    private val threadPages: ThreadPageRepository,
) : ViewModel() {
    /** Null until settings are read, which keeps the splash screen up. */
    val state: StateFlow<MainState?> = combine(settings.settings, settings.isSetupDone, ::MainState)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val account = settings.account.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Whirlpool's site notice from the last API call. */
    val alert = api.alert

    private val _link = MutableStateFlow<Any?>(null)
    /** A route from a link the app was opened with, waiting for the NavHost (which may still be behind setup). */
    val link: StateFlow<Any?> = _link.asStateFlow()

    private val _browserLink = MutableStateFlow<String?>(null)
    /** A claimed deep link (such as a `whrl.pl` redirect outside supported pages) that should open in the browser. */
    val browserLink: StateFlow<String?> = _browserLink.asStateFlow()

    private var linkJob: Job? = null

    fun openLink(route: Any?) {
        if (route != null) {
            linkJob?.cancel()
            _link.value = route
        }
    }

    fun openUrl(url: String) {
        val direct = routeForLink(url)
        if (direct != null) {
            openLink(direct)
            return
        }
        linkJob?.cancel()
        linkJob = viewModelScope.launch {
            when (val target = resolveDeepLink(url, threadPages::resolveShortLink, threadPages::locate)) {
                is DeepLinkTarget.Route -> _link.value = target.route
                is DeepLinkTarget.Browser -> _browserLink.value = target.url
            }
        }
    }

    fun linkHandled() {
        _link.value = null
    }

    fun browserLinkHandled() {
        _browserLink.value = null
    }

    /** Watch actions waiting for Undo, and failures, for the app-wide snackbar. */
    val watchEvents = watched.events

    suspend fun undo(p: PendingWatch) = watched.undo(p)

    fun commit(p: PendingWatch) = watched.commit(p)
}
