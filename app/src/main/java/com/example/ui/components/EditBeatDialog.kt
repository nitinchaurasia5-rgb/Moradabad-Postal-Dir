package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.model.PostalBeat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditBeatDialog(
    beat: PostalBeat?,
    isCreateMode: Boolean,
    allSubOffices: List<String>,
    onSave: (PostalBeat) -> Unit,
    onCreate: (pincode: String, subOffice: String, beatName: String, villagesText: String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pincode by remember { mutableStateOf(beat?.pincode ?: "") }
    var subOffice by remember { mutableStateOf(beat?.subOffice ?: "") }
    var beatName by remember { mutableStateOf(beat?.beatName ?: "") }
    var newVillageInput by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val villagesList = remember {
        mutableStateListOf<String>().apply {
            if (beat != null) {
                addAll(beat.villagesList)
            }
        }
    }

    if (showDeleteConfirm && beat != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Beat Record?") },
            text = { Text("Are you sure you want to remove '${beat.beatName}' and its ${beat.totalVillagesCount} localities from the directory?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(beat.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .heightIn(max = 700.dp)
            .testTag("edit_beat_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isCreateMode) "Add New Beat / BO" else "Edit Postal Beat",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // PIN Code Field
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { if (it.length <= 6) pincode = it },
                    label = { Text("PIN Code (6 digits)") },
                    placeholder = { Text("e.g. 244303") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_pincode_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sub-Office Field
                OutlinedTextField(
                    value = subOffice,
                    onValueChange = { subOffice = it },
                    label = { Text("Sub-Office (SO / HO)") },
                    placeholder = { Text("e.g. Bilari SO, Amroha HO") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_suboffice_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Beat / Branch Office Name Field
                OutlinedTextField(
                    value = beatName,
                    onValueChange = { beatName = it },
                    label = { Text("Branch Office (BO) / Beat Name") },
                    placeholder = { Text("e.g. Saraitarin, Beat no. 1, Chakanwala BO") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_beatname_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Localities Management Section
                Text(
                    text = "Covered Localities & Villages (${villagesList.size}):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Input row to quickly add new village
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newVillageInput,
                        onValueChange = { newVillageInput = it },
                        placeholder = { Text("Village / locality name...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newVillageInput.isNotBlank()) {
                                    val parts = newVillageInput.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                    villagesList.addAll(parts)
                                    newVillageInput = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_village_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (newVillageInput.isNotBlank()) {
                                val parts = newVillageInput.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                villagesList.addAll(parts)
                                newVillageInput = ""
                            }
                        },
                        modifier = Modifier.testTag("add_village_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Editable Village Chips
                if (villagesList.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        villagesList.forEachIndexed { idx, village ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(village, style = MaterialTheme.typography.bodySmall) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (idx < villagesList.size) villagesList.removeAt(idx)
                                        },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No localities added yet. Type a name above or comma-separated list and tap '+' to add.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (!isCreateMode && beat != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Beat Record")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pincode.isNotBlank() && beatName.isNotBlank()) {
                        if (isCreateMode) {
                            onCreate(pincode, subOffice, beatName, villagesList.joinToString(", "))
                        } else if (beat != null) {
                            val updated = beat.copy(
                                pincode = pincode.trim(),
                                subOffice = subOffice.trim(),
                                beatName = beatName.trim(),
                                villagesList = villagesList.toList(),
                                coveredVillagesRaw = villagesList.joinToString(", ")
                            )
                            onSave(updated)
                        }
                    }
                },
                enabled = pincode.isNotBlank() && beatName.isNotBlank(),
                modifier = Modifier.testTag("save_beat_button")
            ) {
                Text(if (isCreateMode) "Create Beat" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
