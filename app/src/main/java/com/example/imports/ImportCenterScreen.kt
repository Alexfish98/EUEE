package com.example.imports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.SectionHeader
import com.example.ui.theme.MonoFontFamily

@Composable
fun ImportCenterScreen(
    viewModel: ImportViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSchemaDialog by remember { mutableStateOf(false) }
    var showReplaceConfirm by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.loadFromUri(it) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("import_center_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Offline Import Center",
                subtitle = "Supports JSON, CSV, and documented TXT up to 10,000 questions/batch",
                actionText = "Schema",
                onActionClick = { showSchemaDialog = true }
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { filePicker.launch(arrayOf("application/json", "text/csv", "text/plain", "*/*")) },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Pick File (SAF)")
                }
                OutlinedButton(
                    onClick = { viewModel.setRawText(ImportParser.DOCUMENTED_JSON_SCHEMA) },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Load Sample JSON")
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.rawText,
                onValueChange = { viewModel.setRawText(it) },
                label = { Text("Paste JSON, CSV, or TXT Question Data") },
                minLines = 5,
                maxLines = 10,
                textStyle = LocalTextStyle.current.copy(fontFamily = MonoFontFamily),
                modifier = Modifier.fillMaxWidth().testTag("import_paste_input")
            )
        }

        item {
            Button(
                onClick = { viewModel.validateAndPreview() },
                enabled = state.rawText.isNotBlank() && !state.isImporting,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("validate_import_btn")
            ) {
                Text("Validate & Preview")
            }
        }

        state.statusMessage?.let { msg ->
            item {
                OutlinedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(msg, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (state.isImporting) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Importing ${state.progressCurrent} / ${state.progressTotal} questions...")
                    LinearProgressIndicator(
                        progress = { if (state.progressTotal == 0) 0f else state.progressCurrent.toFloat() / state.progressTotal },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        state.preview?.let { prev ->
            item {
                ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Import Preview (${prev.formatName})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Valid records: ${prev.validItems.size} • Duplicates flagged: ${prev.duplicateCodes.size} • Rejected: ${prev.rejectedRecords.size}")

                        if (prev.rejectedRecords.isNotEmpty()) {
                            Text("Rejected Records Report:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                            prev.rejectedRecords.take(5).forEach { r ->
                                Text("Row ${r.rowNumber}: ${r.reason}", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Text("Import Strategy", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ImportTargetMode.entries) { m ->
                                FilterChip(
                                    selected = state.targetMode == m,
                                    onClick = { viewModel.setTargetMode(m) },
                                    label = { Text(m.label) }
                                )
                            }
                        }

                        if (state.targetMode == ImportTargetMode.CREATE_NEW) {
                            OutlinedTextField(
                                value = state.newBankName,
                                onValueChange = { viewModel.setNewBankName(it) },
                                label = { Text("New Bank Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(state.banks, key = { it.id }) { bank ->
                                    FilterChip(
                                        selected = state.selectedBankId == bank.id,
                                        onClick = { viewModel.setSelectedBankId(bank.id) },
                                        label = { Text(bank.name) }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (state.targetMode == ImportTargetMode.REPLACE) {
                                    showReplaceConfirm = true
                                } else {
                                    viewModel.executeImport()
                                }
                            },
                            enabled = prev.validItems.isNotEmpty() && !state.isImporting,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("commit_import_btn")
                        ) {
                            Text("Commit Import (${prev.validItems.size} Questions)")
                        }
                    }
                }
            }
        }
    }

    if (showSchemaDialog) {
        AlertDialog(
            onDismissRequest = { showSchemaDialog = false },
            title = { Text("Supported Offline Formats") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("JSON Schema:", fontWeight = FontWeight.Bold)
                    Text(ImportParser.DOCUMENTED_JSON_SCHEMA.take(420) + "...", fontFamily = MonoFontFamily, style = MaterialTheme.typography.bodySmall)
                    Text("TXT Block Format:", fontWeight = FontWeight.Bold)
                    Text(ImportParser.DOCUMENTED_TXT_FORMAT, fontFamily = MonoFontFamily, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { showSchemaDialog = false }) { Text("Close") } }
        )
    }

    if (showReplaceConfirm) {
        AlertDialog(
            onDismissRequest = { showReplaceConfirm = false },
            title = { Text("Confirm Bank Replacement") },
            text = { Text("This will delete all existing questions inside the selected question bank before importing. Continue?") },
            confirmButton = {
                Button(onClick = { showReplaceConfirm = false; viewModel.executeImport() }) { Text("Yes, Replace") }
            },
            dismissButton = { TextButton(onClick = { showReplaceConfirm = false }) { Text("Cancel") } }
        )
    }
}
