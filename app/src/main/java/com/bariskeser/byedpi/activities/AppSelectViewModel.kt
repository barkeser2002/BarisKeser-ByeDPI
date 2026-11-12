package com.bariskeser.byedpi.activities

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bariskeser.byedpi.utility.SplitAppEntry
import com.bariskeser.byedpi.utility.loadSplitLaunchableApps
import kotlinx.coroutines.CancellationException
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
    companion object {
        private val TAG: String = AppSelectViewModel::class.java.simpleName
    }

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
            // A broken package manager entry must not crash the app or leave the
            // spinner up forever. On failure the list is empty and the empty state shows.
            val list = try {
                withContext(Dispatchers.IO) {
                    loadSplitLaunchableApps(context.packageManager, context.packageName)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load launcher apps", e)
                emptyList()
            }
            _apps.value = list
        }
    }
}
