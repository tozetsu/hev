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
import ai.hev.app.domain.choice.MAX_CHOICE_OPTIONS
import ai.hev.app.domain.choice.MAX_SCORE_LEVELS
import ai.hev.app.domain.choice.MIN_CHOICE_OPTIONS
import ai.hev.app.domain.choice.MIN_SCORE_LEVELS
import ai.hev.app.domain.model.QuestionType

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
            item {
                val types = listOf(
                    QuestionType.Choice to R.string.type_choice,
                    QuestionType.Score to R.string.type_score,
                    QuestionType.Noul to R.string.type_noul,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    types.forEachIndexed { index, (type, labelRes) ->
                        SegmentedButton(
                            selected = state.questionType == type,
                            onClick = { vm.setQuestionType(type) },
                            shape = SegmentedButtonDefaults.itemShape(index, types.size),
                        ) {
                            Text(stringResource(labelRes))
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.stateText,
                    onValueChange = vm::setStateText,
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
                    value = state.question,
                    onValueChange = vm::setQuestion,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_question)) },
                    placeholder = { Text(stringResource(R.string.placeholder_question)) },
                    minLines = 2,
                    maxLines = 6,
                    shape = RoundedCornerShape(14.dp),
                )
            }
            if (state.questionType != QuestionType.Noul) {
                item {
                    val maxItems = if (state.questionType == QuestionType.Score) {
                        MAX_SCORE_LEVELS
                    } else {
                        MAX_CHOICE_OPTIONS
                    }
                    val sectionLabel = if (state.questionType == QuestionType.Score) {
                        R.string.label_levels
                    } else {
                        R.string.label_options
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(sectionLabel), style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.questionType == QuestionType.Choice) {
                                TextButton(onClick = { showImportDialog = true }) {
                                    Icon(
                                        Icons.Outlined.UploadFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(stringResource(R.string.action_import))
                                }
                            }
                            TextButton(onClick = vm::addOption, enabled = state.options.size < maxItems) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(stringResource(R.string.action_add))
                            }
                        }
                    }
                }
                items(state.options.size, key = { state.options[it].id }) { index ->
                    val opt = state.options[index]
                    val fieldLabel = if (state.questionType == QuestionType.Score) {
                        stringResource(R.string.level_n, index + 1)
                    } else {
                        stringResource(R.string.option_n, opt.id)
                    }
                    val minSize = if (state.questionType == QuestionType.Score) {
                        MIN_SCORE_LEVELS
                    } else {
                        MIN_CHOICE_OPTIONS
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        OutlinedTextField(
                            value = opt.label,
                            onValueChange = { vm.setOptionLabel(opt.id, it) },
                            modifier = Modifier.weight(1f),
                            label = { Text(fieldLabel) },
                            placeholder = { Text(stringResource(R.string.placeholder_description)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )
                        IconButton(
                            onClick = { vm.removeOption(opt.id) },
                            enabled = state.options.size > minSize,
                        ) {
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
                .menuAnchor(),
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
