package com.kieronquinn.app.darq.ui.screens.settings.apppicker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.darq.BuildConfig
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.components.settings.DarqSharedPreferences
import com.kieronquinn.app.darq.model.settings.AppPickerItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

abstract class SettingsAppPickerViewModel : ViewModel() {

    abstract val loadState: Flow<LoadState>
    abstract val showSearchClearButton: Flow<Boolean>

    abstract fun onPackageEnabledChanged(app: AppPickerItem.App)
    abstract fun setSearchTerm(searchTerm: String)
    abstract fun getSearchTerm(): String
    abstract fun setShowAllApps(showAllApps: Boolean)
    abstract fun getShowAllApps(): Boolean

    sealed class LoadState {
        object Loading : LoadState()
        data class Loaded(val apps: List<AppPickerItem>) : LoadState()
    }

}

class SettingsAppPickerViewModelImpl(
    context: Context,
    private val settings: DarqSharedPreferences
) : SettingsAppPickerViewModel() {

    private val appContext = context.applicationContext

    private val packageManager by lazy {
        appContext.packageManager
    }

    private val allApps = MutableSharedFlow<List<InstalledApp>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply {
        viewModelScope.launch(Dispatchers.IO) {
            emit(packageManager.getInstalledApplications(0).mapNotNull {
                if(it.packageName == BuildConfig.APPLICATION_ID) return@mapNotNull null
                InstalledApp(it.packageName, it.loadLabel(packageManager))
            })
        }
    }

    private val launchableApps = MutableSharedFlow<List<InstalledApp>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply {
        viewModelScope.launch(Dispatchers.IO){
            val apps = allApps.map {
                it.filter { app -> packageManager.getLaunchIntentForPackage(app.packageName) != null }
            }.first()
            emit(apps)
        }
    }

    private val selectedApps = MutableStateFlow<MutableList<String>?>(null).apply {
        viewModelScope.launch(Dispatchers.IO) {
            emit(settings.enabledApps.toMutableList())
        }
    }

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Loading)
    override val loadState = _loadState.asStateFlow()

    private val searchTerm = MutableStateFlow("")

    override val showSearchClearButton: Flow<Boolean> = searchTerm.map { it.isNotEmpty() }

    private val showAllApps = MutableStateFlow(false).apply {
        viewModelScope.launch {
            collect {
                loadApps()
            }
        }
    }

    private suspend fun loadApps() {
        _loadState.emit(LoadState.Loading)
        val items = combine(
            allApps,
            launchableApps,
            selectedApps.filterNotNull(),
            searchTerm
        ) { all, launchable, selected, search ->
            withContext(Dispatchers.IO) {
                val baseApps = if (showAllApps.value) all else launchable
                val mappedApps = baseApps.map {
                    AppPickerItem.App(
                        it.packageName,
                        it.label,
                        selected.contains(it.packageName)
                    )
                }.sortedBy { it.label.toString().lowercase(Locale.getDefault()) }

                val filteredApps = if (search.isNotEmpty()) {
                    val query = search.lowercase(Locale.getDefault())
                    mappedApps.filter {
                        it.label.toString().lowercase(Locale.getDefault()).contains(query) ||
                                it.packageName.lowercase(Locale.getDefault()).contains(query)
                    }
                } else mappedApps

                val enabledApps = filteredApps.filter { it.enabled }
                val disabledApps = filteredApps.filter { !it.enabled }

                val resultList = mutableListOf<AppPickerItem>()
                if (enabledApps.isNotEmpty()) {
                    resultList.add(AppPickerItem.Header(appContext.getString(R.string.app_picker_category_enabled, enabledApps.size)))
                    resultList.addAll(enabledApps)
                }
                if (disabledApps.isNotEmpty()) {
                    resultList.add(AppPickerItem.Header(appContext.getString(R.string.app_picker_category_disabled, disabledApps.size)))
                    resultList.addAll(disabledApps)
                }
                resultList
            }
        }.first()
        _loadState.emit(LoadState.Loaded(items))
    }

    override fun onPackageEnabledChanged(app: AppPickerItem.App) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSelectedApps = selectedApps.value ?: return@launch
            if (app.enabled) {
                if (!currentSelectedApps.contains(app.packageName)) {
                    currentSelectedApps.add(app.packageName)
                }
            } else {
                if (currentSelectedApps.contains(app.packageName)) {
                    currentSelectedApps.remove(app.packageName)
                }
            }
            settings.enabledApps = currentSelectedApps.toTypedArray()
            selectedApps.emit(currentSelectedApps)
            loadApps()
        }
    }

    override fun setSearchTerm(searchTerm: String) {
        viewModelScope.launch {
            this@SettingsAppPickerViewModelImpl.searchTerm.emit(searchTerm)
            loadApps()
        }
    }

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun setShowAllApps(showAllApps: Boolean) {
        viewModelScope.launch {
            this@SettingsAppPickerViewModelImpl.showAllApps.emit(showAllApps)
        }
    }

    override fun getShowAllApps(): Boolean {
        return showAllApps.value
    }

    private data class InstalledApp(val packageName: String, val label: CharSequence)

}