package ai.hev.app.ui.history

import ai.hev.app.domain.history.HistoryEntry
import ai.hev.app.resources.Res
import ai.hev.app.resources.action_cancel
import ai.hev.app.resources.action_delete
import ai.hev.app.resources.cd_back
import ai.hev.app.resources.cd_delete
import ai.hev.app.resources.delete_record_message
import ai.hev.app.resources.delete_record_title
import ai.hev.app.resources.history_detail
import ai.hev.app.resources.loading
import ai.hev.app.ui.common.LocalAppGraph
import ai.hev.app.ui.components.HevIcons
import ai.hev.app.ui.result.ResultContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    historyId: Long,
    onBack: () -> Unit,
) {
    val repo = LocalAppGraph.current.repository
    var entry by remember { mutableStateOf<HistoryEntry?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(historyId) {
        entry = repo.getHistory(historyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.history_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(HevIcons.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(HevIcons.Delete, contentDescription = stringResource(Res.string.cd_delete))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        val e = entry
        if (e != null) {
            ResultContent(entry = e, modifier = Modifier.padding(padding))
        } else {
            Text(
                stringResource(Res.string.loading),
                modifier = Modifier.padding(padding),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.delete_record_title)) },
            text = { Text(stringResource(Res.string.delete_record_message)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repo.deleteHistory(historyId)
                        onBack()
                    }
                }) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}
