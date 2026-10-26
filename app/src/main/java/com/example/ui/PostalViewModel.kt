package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PostalRepository
import com.example.data.local.SyncMetadataEntity
import com.example.model.DashboardStatistics
import com.example.model.PostalBeat
import com.example.model.SortOption
import com.example.model.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PostalUiState(
    val searchQuery: String = "",
    val selectedSubOffice: String? = null,
    val selectedPincode: String? = null,
    val selectedCategory: String? = null, // "BO", "Beat", "Special"
    val onlyBookmarked: Boolean = false,
    val sortOption: SortOption = SortOption.BEAT_NAME,
    val allSubOffices: List<String> = emptyList(),
    val allPincodes: List<String> = emptyList(),
    val editingBeat: PostalBeat? = null,
    val isCreateDialogOpen: Boolean = false,
    val quickAddVillageBeat: PostalBeat? = null,
    val showSyncDialog: Boolean = false,
    val showBulkEditorDialog: Boolean = false,
    val activeTab: Int = 0, // 0: Search & Directory, 1: Visualizations, 2: Sheet Editor
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val snackbarMessage: String? = null
)

class PostalViewModel(private val repository: PostalRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PostalUiState())
    val uiState: StateFlow<PostalUiState> = _uiState.asStateFlow()

    // Room Database stream of postal beats
    val rawBeats: StateFlow<List<PostalBeat>> = repository.beatsState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Room Database stream of sync metadata (offline caching & last sync info)
    val syncMetadata: StateFlow<SyncMetadataEntity?> = repository.syncMetadata
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeSheetUrl: StateFlow<String> = repository.activeSheetUrl

    // Filtered beats flow combining raw beats from Room and UI filter parameters
    val filteredBeats: StateFlow<List<PostalBeat>> = combine(
        rawBeats,
        _uiState
    ) { beats, state ->
        filterAndSortBeats(beats, state)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time statistics computed over filtered and total beats
    val statistics: StateFlow<DashboardStatistics> = combine(
        rawBeats,
        filteredBeats
    ) { allBeats, filtered ->
        computeStatistics(allBeats, filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStatistics())

    init {
        viewModelScope.launch {
            repository.initialize()
        }

        viewModelScope.launch {
            rawBeats.collect { beats ->
                val subOffices = beats.map { it.subOffice }.distinct().sorted()
                val pincodes = beats.map { it.pincode }.distinct().sorted()
                _uiState.value = _uiState.value.copy(
                    allSubOffices = subOffices,
                    allPincodes = pincodes
                )
            }
        }
    }

    private fun filterAndSortBeats(beats: List<PostalBeat>, state: PostalUiState): List<PostalBeat> {
        val query = state.searchQuery.trim().lowercase()

        var filtered = beats.filter { beat ->
            // Sub-Office filter
            if (state.selectedSubOffice != null && beat.subOffice != state.selectedSubOffice) {
                return@filter false
            }
            // PIN code filter
            if (state.selectedPincode != null && beat.pincode != state.selectedPincode) {
                return@filter false
            }
            // Bookmarks only filter
            if (state.onlyBookmarked && !beat.isBookmarked) {
                return@filter false
            }
            // Category filter
            if (state.selectedCategory != null) {
                val matchesCategory = when (state.selectedCategory) {
                    "BO" -> beat.isBranchOffice
                    "Beat" -> beat.isNumberedBeat
                    "Special" -> beat.isGovtOrSpecial
                    else -> true
                }
                if (!matchesCategory) return@filter false
            }

            // Text search query across village names, beat name, sub-office, and pin code
            if (query.isNotEmpty()) {
                val matchesPincode = beat.pincode.lowercase().contains(query)
                val matchesSubOffice = beat.subOffice.lowercase().contains(query)
                val matchesBeatName = beat.beatName.lowercase().contains(query)
                val matchesVillages = beat.villagesList.any { it.lowercase().contains(query) }
                val matchesRaw = beat.coveredVillagesRaw.lowercase().contains(query)

                matchesPincode || matchesSubOffice || matchesBeatName || matchesVillages || matchesRaw
            } else {
                true
            }
        }

        filtered = when (state.sortOption) {
            SortOption.BEAT_NAME -> filtered.sortedBy { it.beatName.lowercase() }
            SortOption.VILLAGE_COUNT_DESC -> filtered.sortedByDescending { it.totalVillagesCount }
            SortOption.SUB_OFFICE -> filtered.sortedBy { it.subOffice.lowercase() }
            SortOption.PINCODE -> filtered.sortedBy { it.pincode }
        }

        return filtered
    }

    private fun computeStatistics(allBeats: List<PostalBeat>, filtered: List<PostalBeat>): DashboardStatistics {
        val subOfficesCount = allBeats.map { it.subOffice }.distinct().size
        val pincodesCount = allBeats.map { it.pincode }.distinct().size
        val totalVillagesCount = allBeats.sumOf { it.totalVillagesCount }

        val topSOs = allBeats.groupingBy { it.subOffice }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(8)

        val topPINs = allBeats.groupingBy { it.pincode }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(6)

        val boCount = filtered.count { it.isBranchOffice }
        val numberedCount = filtered.count { it.isNumberedBeat }
        val specialCount = filtered.count { it.isGovtOrSpecial }

        return DashboardStatistics(
            totalSubOffices = subOfficesCount,
            totalBeats = allBeats.size,
            filteredMatches = filtered.size,
            totalPincodes = pincodesCount,
            totalVillages = totalVillagesCount,
            topSubOffices = topSOs,
            topPincodes = topPINs,
            branchOfficeCount = boCount,
            numberedBeatCount = numberedCount,
            specialOfficeCount = specialCount
        )
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onSubOfficeSelected(subOffice: String?) {
        _uiState.value = _uiState.value.copy(selectedSubOffice = subOffice)
    }

    fun onPincodeSelected(pincode: String?) {
        _uiState.value = _uiState.value.copy(selectedPincode = pincode)
    }

    fun onCategorySelected(category: String?) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = if (_uiState.value.selectedCategory == category) null else category
        )
    }

    fun onToggleBookmarksFilter() {
        _uiState.value = _uiState.value.copy(onlyBookmarked = !_uiState.value.onlyBookmarked)
    }

    fun onSortOptionChanged(sortOption: SortOption) {
        _uiState.value = _uiState.value.copy(sortOption = sortOption)
    }

    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            selectedSubOffice = null,
            selectedPincode = null,
            selectedCategory = null,
            onlyBookmarked = false
        )
    }

    fun setActiveTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tabIndex)
    }

    fun toggleBookmark(beatId: String) {
        viewModelScope.launch {
            val beat = rawBeats.value.find { it.id == beatId } ?: return@launch
            repository.toggleBookmark(beatId, beat.isBookmarked)
        }
    }

    fun openEditBeatDialog(beat: PostalBeat) {
        _uiState.value = _uiState.value.copy(editingBeat = beat, isCreateDialogOpen = false)
    }

    fun openCreateBeatDialog() {
        _uiState.value = _uiState.value.copy(isCreateDialogOpen = true, editingBeat = null)
    }

    fun closeEditDialog() {
        _uiState.value = _uiState.value.copy(editingBeat = null, isCreateDialogOpen = false)
    }

    fun openQuickAddVillageDialog(beat: PostalBeat) {
        _uiState.value = _uiState.value.copy(quickAddVillageBeat = beat)
    }

    fun closeQuickAddVillageDialog() {
        _uiState.value = _uiState.value.copy(quickAddVillageBeat = null)
    }

    fun openSyncDialog() {
        _uiState.value = _uiState.value.copy(showSyncDialog = true)
    }

    fun closeSyncDialog() {
        _uiState.value = _uiState.value.copy(showSyncDialog = false)
    }

    fun openBulkEditorDialog() {
        _uiState.value = _uiState.value.copy(showBulkEditorDialog = true)
    }

    fun closeBulkEditorDialog() {
        _uiState.value = _uiState.value.copy(showBulkEditorDialog = false)
    }

    fun saveBeat(beat: PostalBeat) {
        viewModelScope.launch {
            repository.saveBeat(beat)
            closeEditDialog()
            _uiState.value = _uiState.value.copy(snackbarMessage = "Beat saved: ${beat.beatName}")
        }
    }

    fun createBeat(pincode: String, subOffice: String, beatName: String, villagesText: String) {
        viewModelScope.launch {
            val villagesList = villagesText.split(Regex("[,;\\n]+"))
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val newBeat = repository.addBeat(pincode, subOffice, beatName, villagesList)
            closeEditDialog()
            _uiState.value = _uiState.value.copy(snackbarMessage = "Added new beat: ${newBeat.beatName}")
        }
    }

    fun deleteBeat(beatId: String) {
        viewModelScope.launch {
            repository.deleteBeat(beatId)
            closeEditDialog()
            _uiState.value = _uiState.value.copy(snackbarMessage = "Beat removed from directory")
        }
    }

    fun addVillageToBeat(beatId: String, newVillageName: String) {
        if (newVillageName.isBlank()) return
        viewModelScope.launch {
            val beat = rawBeats.value.find { it.id == beatId } ?: return@launch
            repository.addVillageToBeat(beatId, newVillageName.trim(), beat)
            closeQuickAddVillageDialog()
            _uiState.value = _uiState.value.copy(snackbarMessage = "Added locality '$newVillageName'")
        }
    }

    fun removeVillageFromBeat(beatId: String, villageName: String) {
        viewModelScope.launch {
            val beat = rawBeats.value.find { it.id == beatId } ?: return@launch
            repository.removeVillageFromBeat(beatId, villageName, beat)
            _uiState.value = _uiState.value.copy(snackbarMessage = "Removed locality '$villageName'")
        }
    }

    fun syncGoogleSheet(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.Syncing)
            val result = repository.syncFromGoogleSheet(url)
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _uiState.value = _uiState.value.copy(
                    syncStatus = SyncStatus.Success("Synced $count beats to local Room cache", count, url, System.currentTimeMillis()),
                    snackbarMessage = "Synced & cached $count beats in Room database"
                )
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown sync error"
                _uiState.value = _uiState.value.copy(
                    syncStatus = SyncStatus.Error(errorMsg),
                    snackbarMessage = errorMsg
                )
            }
        }
    }

    fun importCsv(csvContent: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.Syncing)
            val result = repository.replaceWithCsv(csvContent)
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _uiState.value = _uiState.value.copy(
                    syncStatus = SyncStatus.Success("Imported $count beats to Room cache", count, "Direct CSV Paste", System.currentTimeMillis()),
                    snackbarMessage = "Imported & cached $count records",
                    showBulkEditorDialog = false
                )
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Import error"
                _uiState.value = _uiState.value.copy(
                    syncStatus = SyncStatus.Error(errorMsg),
                    snackbarMessage = "Import failed: $errorMsg"
                )
            }
        }
    }

    fun resetToBundledData() {
        viewModelScope.launch {
            repository.resetToDefault()
            _uiState.value = _uiState.value.copy(
                syncStatus = SyncStatus.Idle,
                snackbarMessage = "Reset Room database to original master data"
            )
        }
    }

    fun getExportCsv(): String {
        return repository.generateCsv(rawBeats.value)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
