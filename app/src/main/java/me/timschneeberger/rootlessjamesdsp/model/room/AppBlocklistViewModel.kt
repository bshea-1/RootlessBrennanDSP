package me.timschneeberger.rootlessjamesdsp.model.room

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class AppBlocklistViewModel(private val repository: AppBlocklistRepository) : ViewModel() {

    val blockedApps: LiveData<List<BlockedApp>> = repository.blocklist.asLiveData()

    fun insert(app: BlockedApp) = viewModelScope.launch {
        repository.insert(app)
    }

    fun delete(word: BlockedApp) = viewModelScope.launch {
        repository.delete(word)
    }
}

class AppBlocklistViewModelFactory(private val repository: AppBlocklistRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppBlocklistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppBlocklistViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
