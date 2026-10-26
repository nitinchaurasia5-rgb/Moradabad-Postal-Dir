package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SortOption
import com.example.ui.PostalViewModel
import com.example.ui.components.BulkDataEditorDialog
import com.example.ui.components.EditBeatDialog
import com.example.ui.components.GoogleSheetSyncDialog
import com.example.ui.components.InteractiveCharts
import com.example.ui.components.MetricKpiCard
import com.example.ui.components.PostalBeatCard
import com.example.ui.components.QuickAddVillageDialog
import com.example.ui.theme.PostalBlue
import com.example.ui.theme.PostalBlueDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PostalViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredBeats by viewModel.filteredBeats.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val syncMetadata by viewModel.syncMetadata.collectAsState()
    val activeSheetUrl by viewModel.activeSheetUrl.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var showSubOfficeMenu by remember { mutableStateOf(false) }
    var showPincodeMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val lastSyncDisplay = syncMetadata?.let {
        "Room Cache: ${it.sourceName} • ${it.formattedLastSync}"
    } ?: "Room Cache: Initializing..."

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Postal Beats",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Offline Ready",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = lastSyncDisplay,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Google Sheet Sync Action
                    IconButton(
                        onClick = { viewModel.openSyncDialog() },
                        modifier = Modifier.testTag("top_sync_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sync Google Sheet",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Direct Spreadsheet Editor Action
                    IconButton(
                        onClick = { viewModel.openBulkEditorDialog() },
                        modifier = Modifier.testTag("top_bulk_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Edit Data / CSV",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateBeatDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_beat_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Beat")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row: Directory / Search, Visualizations, Data Editor
            TabRow(
                selectedTabIndex = uiState.activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = uiState.activeTab == 0,
                    onClick = { viewModel.setActiveTab(0) },
                    text = { Text("Search & Directory", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.ViewList, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_directory")
                )
                Tab(
                    selected = uiState.activeTab == 1,
                    onClick = { viewModel.setActiveTab(1) },
                    text = { Text("Visualizations", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_visualizations")
                )
                Tab(
                    selected = uiState.activeTab == 2,
                    onClick = { viewModel.setActiveTab(2) },
                    text = { Text("Data Editor", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_data_editor")
                )
            }

            when (uiState.activeTab) {
                0 -> {
                    // Search & Directory Main View
                    DirectoryView(
                        viewModel = viewModel,
                        uiState = uiState,
                        filteredBeats = filteredBeats,
                        statistics = statistics,
                        listState = listState,
                        showSubOfficeMenu = showSubOfficeMenu,
                        onShowSubOfficeMenuChange = { showSubOfficeMenu = it },
                        showPincodeMenu = showPincodeMenu,
                        onShowPincodeMenuChange = { showPincodeMenu = it },
                        showSortMenu = showSortMenu,
                        onShowSortMenuChange = { showSortMenu = it }
                    )
                }
                1 -> {
                    // Full Visualizations & Insights Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        InteractiveCharts(
                            statistics = statistics,
                            selectedSubOffice = uiState.selectedSubOffice,
                            selectedPincode = uiState.selectedPincode,
                            onSubOfficeSelected = {
                                viewModel.onSubOfficeSelected(it)
                                viewModel.setActiveTab(0) // jump back to results
                            },
                            onPincodeSelected = {
                                viewModel.onPincodeSelected(it)
                                viewModel.setActiveTab(0) // jump back to results
                            }
                        )
                    }
                }
                2 -> {
                    // Data Editor Management Hub Tab
                    DataEditorHubTab(
                        viewModel = viewModel,
                        lastSyncInfo = lastSyncDisplay,
                        totalBeats = statistics.totalBeats,
                        totalVillages = statistics.totalVillages
                    )
                }
            }
        }
    }

    // Modal Dialogs
    if (uiState.editingBeat != null || uiState.isCreateDialogOpen) {
        EditBeatDialog(
            beat = uiState.editingBeat,
            isCreateMode = uiState.isCreateDialogOpen,
            allSubOffices = uiState.allSubOffices,
            onSave = { updated -> viewModel.saveBeat(updated) },
            onCreate = { pin, so, beat, villages ->
                viewModel.createBeat(pin, so, beat, villages)
            },
            onDelete = { id -> viewModel.deleteBeat(id) },
            onDismiss = { viewModel.closeEditDialog() }
        )
    }

    if (uiState.quickAddVillageBeat != null) {
        QuickAddVillageDialog(
            beat = uiState.quickAddVillageBeat!!,
            onAddVillage = { beatId, name -> viewModel.addVillageToBeat(beatId, name) },
            onDismiss = { viewModel.closeQuickAddVillageDialog() }
        )
    }

    if (uiState.showSyncDialog) {
        GoogleSheetSyncDialog(
            initialUrl = activeSheetUrl,
            lastSyncInfo = lastSyncDisplay,
            syncStatus = uiState.syncStatus,
            onSync = { url -> viewModel.syncGoogleSheet(url) },
            onResetDefault = { viewModel.resetToBundledData() },
            onDismiss = { viewModel.closeSyncDialog() }
        )
    }

    if (uiState.showBulkEditorDialog) {
        BulkDataEditorDialog(
            initialCsv = viewModel.getExportCsv(),
            onImportCsv = { csv -> viewModel.importCsv(csv) },
            onDismiss = { viewModel.closeBulkEditorDialog() }
        )
    }
}

@Composable
private fun DirectoryView(
    viewModel: PostalViewModel,
    uiState: com.example.ui.PostalUiState,
    filteredBeats: List<com.example.model.PostalBeat>,
    statistics: com.example.model.DashboardStatistics,
    listState: androidx.compose.foundation.lazy.LazyListState,
    showSubOfficeMenu: Boolean,
    onShowSubOfficeMenuChange: (Boolean) -> Unit,
    showPincodeMenu: Boolean,
    onShowPincodeMenuChange: (Boolean) -> Unit,
    showSortMenu: Boolean,
    onShowSortMenuChange: (Boolean) -> Unit
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. KPI Metric Summary Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricKpiCard(
                        title = "Sub-Offices",
                        value = "${statistics.totalSubOffices}",
                        icon = Icons.Default.LocationCity,
                        iconTint = Color(0xFF1E88E5),
                        modifier = Modifier.width(135.dp)
                    )
                    MetricKpiCard(
                        title = "Delivery Beats",
                        value = "${statistics.totalBeats}",
                        icon = Icons.Default.Mail,
                        iconTint = Color(0xFF00897B),
                        badgeText = "${statistics.filteredMatches} active",
                        modifier = Modifier.width(155.dp)
                    )
                    MetricKpiCard(
                        title = "PIN Codes",
                        value = "${statistics.totalPincodes}",
                        icon = Icons.Default.PinDrop,
                        iconTint = Color(0xFF673AB7),
                        modifier = Modifier.width(135.dp)
                    )
                    MetricKpiCard(
                        title = "Indexed Localities",
                        value = "${statistics.totalVillages}",
                        icon = Icons.Default.LocationOn,
                        iconTint = Color(0xFFE65100),
                        modifier = Modifier.width(160.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sticky-style Search Field
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "Search villages (e.g. Mangalpura, नखासा, Bilaspur)...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("locality_search_bar")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter & Sort Chip Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sub-Office Filter Chip with Dropdown
                    Box {
                        FilterChip(
                            selected = uiState.selectedSubOffice != null,
                            onClick = { onShowSubOfficeMenuChange(true) },
                            label = {
                                Text(uiState.selectedSubOffice ?: "Sub-Office (All)")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("filter_suboffice_chip")
                        )
                        DropdownMenu(
                            expanded = showSubOfficeMenu,
                            onDismissRequest = { onShowSubOfficeMenuChange(false) }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Sub-Offices (${uiState.allSubOffices.size})") },
                                onClick = {
                                    viewModel.onSubOfficeSelected(null)
                                    onShowSubOfficeMenuChange(false)
                                }
                            )
                            uiState.allSubOffices.forEach { so ->
                                DropdownMenuItem(
                                    text = { Text(so) },
                                    onClick = {
                                        viewModel.onSubOfficeSelected(so)
                                        onShowSubOfficeMenuChange(false)
                                    }
                                )
                            }
                        }
                    }

                    // PIN Code Filter Chip with Dropdown
                    Box {
                        FilterChip(
                            selected = uiState.selectedPincode != null,
                            onClick = { onShowPincodeMenuChange(true) },
                            label = {
                                Text(uiState.selectedPincode ?: "PIN Code (All)")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("filter_pincode_chip")
                        )
                        DropdownMenu(
                            expanded = showPincodeMenu,
                            onDismissRequest = { onShowPincodeMenuChange(false) }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All PIN Codes (${uiState.allPincodes.size})") },
                                onClick = {
                                    viewModel.onPincodeSelected(null)
                                    onShowPincodeMenuChange(false)
                                }
                            )
                            uiState.allPincodes.forEach { pin ->
                                DropdownMenuItem(
                                    text = { Text(pin) },
                                    onClick = {
                                        viewModel.onPincodeSelected(pin)
                                        onShowPincodeMenuChange(false)
                                    }
                                )
                            }
                        }
                    }

                    // Quick Filter: Branch Offices
                    FilterChip(
                        selected = uiState.selectedCategory == "BO",
                        onClick = { viewModel.onCategorySelected("BO") },
                        label = { Text("Branch Offices") }
                    )

                    // Quick Filter: Numbered Beats
                    FilterChip(
                        selected = uiState.selectedCategory == "Beat",
                        onClick = { viewModel.onCategorySelected("Beat") },
                        label = { Text("Town Beats") }
                    )

                    // Quick Filter: Starred / Bookmarks
                    FilterChip(
                        selected = uiState.onlyBookmarked,
                        onClick = { viewModel.onToggleBookmarksFilter() },
                        label = { Text("Saved") },
                        leadingIcon = {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )

                    // Sort Dropdown
                    Box {
                        FilterChip(
                            selected = uiState.sortOption != SortOption.BEAT_NAME,
                            onClick = { onShowSortMenuChange(true) },
                            label = { Text(uiState.sortOption.label) },
                            leadingIcon = {
                                Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { onShowSortMenuChange(false) }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        viewModel.onSortOptionChanged(option)
                                        onShowSortMenuChange(false)
                                    }
                                )
                            }
                        }
                    }

                    // Reset Filters
                    if (uiState.selectedSubOffice != null || uiState.selectedPincode != null ||
                        uiState.selectedCategory != null || uiState.onlyBookmarked || uiState.searchQuery.isNotBlank()) {
                        TextButton(onClick = { viewModel.clearAllFilters() }) {
                            Text("Clear Filters", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Results Count Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (uiState.searchQuery.isNotBlank())
                            "Found ${filteredBeats.size} matching beats for '${uiState.searchQuery}'"
                        else
                            "Showing ${filteredBeats.size} of ${statistics.totalBeats} delivery beats",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (uiState.searchQuery.isNotBlank()) {
                        Text(
                            text = "⚡ Real-time highlight active",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
        }

        // 2. Filtered Beats List
        if (filteredBeats.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching postal beats found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try adjusting your query, clearing sub-office filters, or add a new locality.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.clearAllFilters() }) {
                                Text("Clear All Filters")
                            }
                            OutlinedButton(onClick = { viewModel.openCreateBeatDialog() }) {
                                Text("+ Add New Beat")
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = filteredBeats,
                key = { it.id }
            ) { beat ->
                PostalBeatCard(
                    beat = beat,
                    searchQuery = uiState.searchQuery,
                    onEditClick = { viewModel.openEditBeatDialog(it) },
                    onQuickAddVillage = { viewModel.openQuickAddVillageDialog(it) },
                    onToggleBookmark = { viewModel.toggleBookmark(it) }
                )
            }
        }
    }
}

@Composable
private fun DataEditorHubTab(
    viewModel: PostalViewModel,
    lastSyncInfo: String,
    totalBeats: Int,
    totalVillages: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Easy Data Management Hub",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Current status: $lastSyncInfo\nTotal beats: $totalBeats | Total indexed localities: $totalVillages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }

        // Action 1: Add Single Beat
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Single Beat / BO",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Create a new postal beat record with PIN code, sub-office, and covered localities.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.openCreateBeatDialog() },
                    modifier = Modifier.fillMaxWidth().testTag("hub_add_beat_button")
                ) {
                    Text("+ Add New Beat")
                }
            }
        }

        // Action 2: Direct Spreadsheet & CSV Text Editor
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bulk Spreadsheet & CSV Editor",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Copy the entire directory to clipboard to open in Excel/Google Sheets, or paste updated CSV rows directly into the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.openBulkEditorDialog() },
                    modifier = Modifier.fillMaxWidth().testTag("hub_bulk_editor_button")
                ) {
                    Text("Open CSV / Spreadsheet Editor")
                }
            }
        }

        // Action 3: Google Sheets Live Sync
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = Color(0xFF00897B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Sheets Live Sync",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Connect a published Google Sheet CSV URL to fetch live updates from Google Drive automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.openSyncDialog() },
                    modifier = Modifier.fillMaxWidth().testTag("hub_sheet_sync_button")
                ) {
                    Text("Configure Google Sheet Sync")
                }
            }
        }

        // Action 4: Reset Data
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Reset Dataset",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Discard local modifications and restore the bundled master directory of 455 beats.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { viewModel.resetToBundledData() },
                    modifier = Modifier.fillMaxWidth().testTag("hub_reset_button")
                ) {
                    Text("Restore Default Master Data")
                }
            }
        }
    }
}
