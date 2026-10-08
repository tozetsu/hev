package ai.hev.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.hev.app.HevApp
import ai.hev.app.R
import ai.hev.app.domain.decision.DecisionKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onResult: (Long) -> Unit,
) {
    val app = LocalContext.current.applicationContext as HevApp
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app, app.container.repository))
    val state by vm.uiState.collectAsStateWithLifecycle()
    val notConfigured = stringResource(R.string.provider_not_configured)
    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeContent,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Outlined.History, contentDescription = stringResource(R.string.cd_history))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ProviderPicker(
                    providers = state.providers.map { it.id to it.name },
                    selectedId = state.activeProvider?.id,
                    selectedLabel = state.activeProvider?.let { "${it.name} · ${it.model}" } ?: notConfigured,
                    onSelect = vm::selectProvider,
                )
            }
            val kinds = DecisionKind.entries.filter(state.capabilities::supports)
            if (kinds.size > 1) {
                item {
                    KindSelector(kinds = kinds, selected = state.draft.kind, onSelect = vm::setKind)
                }
            }
            item {
                OutlinedTextField(
                    value = state.draft.context,
                    onValueChange = vm::setContext,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_state)) },
                    placeholder = { Text(stringResource(R.string.placeholder_optional)) },
                    minLines = 2,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                )
            }
            item {
                OutlinedTextField(
                    value = state.draft.instructions,
                    onValueChange = vm::setInstructions,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_question)) },
                    placeholder = { Text(stringResource(R.string.placeholder_question)) },
                    minLines = 2,
                    maxLines = 6,
                    shape = RoundedCornerShape(14.dp),
                )
            }
            if (state.itemRange != null) {
                val isScore = state.draft.kind == DecisionKind.Score
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(if (isScore) R.string.label_levels else R.string.label_options),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isScore) {
                                TextButton(onClick = { showImportDialog = true }) {
                                    Icon(
                                        Icons.Outlined.UploadFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(stringResource(R.string.action_import))
                                }
                            }
                            TextButton(onClick = vm::addItem, enabled = state.canAddItem) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(stringResource(R.string.action_add))
                            }
                        }
                    }
                }
                itemsIndexed(state.draft.items, key = { _, item -> item.id }) { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        OutlinedTextField(
                            value = item.label,
                            onValueChange = { vm.setItemLabel(item.id, it) },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    if (isScore) stringResource(R.string.level_n, index + 1)
                                    else stringResource(R.string.option_n, item.id),
                                )
                            },
                            placeholder = { Text(stringResource(R.string.placeholder_description)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )
                        IconButton(onClick = { vm.removeItem(item.id) }, enabled = state.canRemoveItem) {
                            Icon(
                                Icons.Outlined.RemoveCircleOutline,
                                contentDescription = stringResource(R.string.cd_delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Button(
                    onClick = { vm.submit(onResult) },
                    enabled = !state.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.action_submit), style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showImportDialog) {
        ImportOptionsDialog(
            onDismiss = { showImportDialog = false },
            onImport = { text ->
                if (vm.importChoiceOptions(text)) {
                    showImportDialog = false
                }
            },
        )
    }
}

@Composable
private fun ImportOptionsDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_options_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    placeholder = { Text(stringResource(R.string.import_options_hint)) },
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(text) },
                enabled = text.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderPicker(
    providers: List<Pair<String, String>>,
    selectedId: String?,
    selectedLabel: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.provider)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(14.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            providers.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun KindSelector(
    kinds: List<DecisionKind>,
    selected: DecisionKind,
    onSelect: (DecisionKind) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        kinds.forEachIndexed { index, kind ->
            SegmentedButton(
                selected = kind == selected,
                onClick = { onSelect(kind) },
                shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
            ) {
                Text(stringResource(kind.labelRes))
            }
        }
    }
}

private val DecisionKind.labelRes: Int
    get() = when (this) {
        DecisionKind.Choice -> R.string.type_choice
        DecisionKind.Score -> R.string.type_score
        DecisionKind.YesNo -> R.string.type_yes_no
    }
