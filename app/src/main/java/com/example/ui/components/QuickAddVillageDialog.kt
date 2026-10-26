package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.model.PostalBeat

@Composable
fun QuickAddVillageDialog(
    beat: PostalBeat,
    onAddVillage: (beatId: String, villageName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var villageName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Locality to ${beat.beatName}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PIN: ${beat.pincode} • ${beat.subOffice}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = villageName,
                    onValueChange = { villageName = it },
                    label = { Text("Village / Locality Name") },
                    placeholder = { Text("e.g. Rampur Majhra, नया गाँव") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (villageName.isNotBlank()) {
                                onAddVillage(beat.id, villageName)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_add_village_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (villageName.isNotBlank()) {
                        onAddVillage(beat.id, villageName)
                    }
                },
                enabled = villageName.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_village_button")
            ) {
                Text("Add Locality")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
