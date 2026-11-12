package com.bariskeser.byedpi.activities

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bariskeser.byedpi.utility.SplitAppEntry
import com.bariskeser.byedpi.utility.loadSplitLaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads the launcher apps once and keeps them across configuration changes, so a
 * rotation does not reload the list and the list keeps its scroll position.
 */
class AppSelectViewModel(application: Application) : AndroidViewModel(application) {
    // null while loading, then the full (unfiltered) list.
    private val _apps = MutableStateFlow<List<SplitAppEntry>?>(null)
    val apps: StateFlow<List<SplitAppEntry>?> = _apps.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    private fun load() {
        if (loadJob != null) return
        val context = getApplication<Application>()
        loadJob = viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                loadSplitLaunchableApps(context.packageManager, context.packageName)
            }
            _apps.value = list
        }
    }
}
