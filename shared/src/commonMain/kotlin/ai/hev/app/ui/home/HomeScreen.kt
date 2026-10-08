package ai.hev.app.ui.home

import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.resources.Res
import ai.hev.app.resources.action_add
import ai.hev.app.resources.action_cancel
import ai.hev.app.resources.action_import
import ai.hev.app.resources.action_submit
import ai.hev.app.resources.app_name
import ai.hev.app.resources.cd_delete
import ai.hev.app.resources.cd_history
import ai.hev.app.resources.cd_settings
import ai.hev.app.resources.error_configure_provider
import ai.hev.app.resources.error_import_min
import ai.hev.app.resources.import_options_hint
import ai.hev.app.resources.import_options_title
import ai.hev.app.resources.label_levels
import ai.hev.app.resources.label_options
import ai.hev.app.resources.label_question
import ai.hev.app.resources.label_state
import ai.hev.app.resources.level_n
import ai.hev.app.resources.option_n
import ai.hev.app.resources.placeholder_description
import ai.hev.app.resources.placeholder_optional
import ai.hev.app.resources.placeholder_question
import ai.hev.app.resources.provider
import ai.hev.app.resources.provider_not_configured
import ai.hev.app.resources.type_choice
import ai.hev.app.resources.type_score
import ai.hev.app.resources.type_yes_no
import ai.hev.app.ui.common.LocalAppGraph
import ai.hev.app.ui.common.text
import ai.hev.app.ui.components.HevIcons
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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onResult: (Long) -> Unit,
) {
    val repo = LocalAppGraph.current.repository
    val vm = viewModel { HomeViewModel(repo) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val notConfigured = stringResource(Res.string.provider_not_configured)
    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeContent,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(Res.string.app_name), fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(HevIcons.History, contentDescription = stringResource(Res.string.cd_history))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(HevIcons.Settings, contentDescription = stringResource(Res.string.cd_settings))
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
                    label = { Text(stringResource(Res.string.label_state)) },
                    placeholder = { Text(stringResource(Res.string.placeholder_optional)) },
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
                    label = { Text(stringResource(Res.string.label_question)) },
                    placeholder = { Text(stringResource(Res.string.placeholder_question)) },
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
                            stringResource(if (isScore) Res.string.label_levels else Res.string.label_options),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isScore) {
                                TextButton(onClick = { showImportDialog = true }) {
                                    Icon(
                                        HevIcons.UploadFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(stringResource(Res.string.action_import))
                                }
                            }
                            TextButton(onClick = vm::addItem, enabled = state.canAddItem) {
                                Icon(HevIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(stringResource(Res.string.action_add))
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
                                    if (isScore) stringResource(Res.string.level_n, index + 1)
                                    else stringResource(Res.string.option_n, item.id),
                                )
                            },
                            placeholder = { Text(stringResource(Res.string.placeholder_description)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )
                        IconButton(onClick = { vm.removeItem(item.id) }, enabled = state.canRemoveItem) {
                            Icon(
                                HevIcons.RemoveCircleOutline,
                                contentDescription = stringResource(Res.string.cd_delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                state.error?.let { error ->
                    Text(
                        text = error.text(),
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
                        Text(stringResource(Res.string.action_submit), style = MaterialTheme.typography.titleMedium)
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
        title = { Text(stringResource(Res.string.import_options_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    placeholder = { Text(stringResource(Res.string.import_options_hint)) },
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(text) },
                enabled = text.isNotBlank(),
            ) {
                Text(stringResource(Res.string.action_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
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
            label = { Text(stringResource(Res.string.provider)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
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

private val DecisionKind.labelRes: StringResource
    get() = when (this) {
        DecisionKind.Choice -> Res.string.type_choice
        DecisionKind.Score -> Res.string.type_score
        DecisionKind.YesNo -> Res.string.type_yes_no
    }

@Composable
private fun HomeError.text(): String = when (this) {
    HomeError.NoProvider -> stringResource(Res.string.error_configure_provider)
    is HomeError.Provider -> issue.text()
    is HomeError.Draft -> issue.text()
    is HomeError.TooFewOptions -> stringResource(Res.string.error_import_min, min)
    is HomeError.Request -> error.text()
}
