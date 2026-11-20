package com.example.exports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.SectionHeader

@Composable
fun ExportCenterScreen(
    viewModel: ExportViewModel,
    modifier: Modifier = Modifier
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingExportType by remember { mutableStateOf<ExportPayloadType?>(null) }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val type = pendingExportType
        if (uri != null && type != null) {
            viewModel.exportToUri(type, uri)
        }
        pendingExportType = null
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreBackupFromUri(it) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("export_center_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Export & Offline Backup Center",
                subtitle = "Native Android Storage Access Framework, PDF generation & Share Sheet"
            )
        }

        status?.let { msg ->
            item {
                ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(msg, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Restore Full Offline Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Pick a previously exported euee_mastery_backup.json file", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "*/*")) }) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Restore")
                    }
                }
            }
        }

        items(ExportPayloadType.entries) { type ->
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(type.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(type.fileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            pendingExportType = type
                            createDocLauncher.launch(type.fileName)
                        }) {
                            Text("Save")
                        }
                        IconButton(onClick = { viewModel.prepareAndShare(context, type) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share ${type.title}")
                        }
                    }
                }
            }
        }
    }
}
