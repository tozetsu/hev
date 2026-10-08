package ai.hev.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.hev.app.R
import ai.hev.app.ui.common.LocalAppGraph
import ai.hev.app.ui.common.text
import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.ProviderPreset
import ai.hev.app.domain.provider.ProviderPresets
import ai.hev.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditScreen(
    providerId: String,
    onBack: () -> Unit,
) {
    val repo = LocalAppGraph.current.repository
    val vm = viewModel(key = providerId) {
        ProviderEditViewModel(repo, providerId.takeUnless { it == Routes.NEW_PROVIDER })
    }
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeContent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.provider_new else R.string.provider_edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.cd_delete))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                PresetChips(selected = state.preset, onSelect = vm::selectPreset)
            }
            if (state.isCustom) {
                item {
                    ProtocolSelector(
                        selected = state.protocol,
                        onSelect = vm::setProtocol,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            item {
                Field(state.name, vm::setName, R.string.label_name)
            }
            item {
                Field(state.endpoint, vm::setEndpoint, R.string.label_endpoint, keyboardType = KeyboardType.Uri)
            }
            if (state.isCustom) {
                item {
                    Field(state.modelsUrl, vm::setModelsUrl, R.string.label_models_url, keyboardType = KeyboardType.Uri)
                }
            }
            item {
                SecretField(
                    value = state.apiKey,
                    onValueChange = vm::setApiKey,
                    label = if (state.apiKeyRequired) R.string.label_api_key else R.string.label_api_key_optional,
                )
            }
            item {
                ModelField(
                    value = state.model,
                    options = state.modelOptions,
                    onValueChange = vm::setModel,
                    onOpen = vm::loadModels,
                )
            }
            item {
                state.error?.let {
                    Text(
                        text = it.text(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                Button(
                    onClick = { if (vm.save()) onBack() },
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_provider_title)) },
            text = { Text(stringResource(R.string.delete_provider_message, state.name)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete()
                    confirmDelete = false
                    onBack()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun PresetChips(selected: ProviderPreset?, onSelect: (ProviderPreset?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ProviderPresets.all, key = { it.id }) { preset ->
            FilterChip(
                selected = preset == selected,
                onClick = { onSelect(preset) },
                label = { Text(preset.name) },
            )
        }
        item(key = "custom") {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.preset_custom)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProtocolSelector(
    selected: DecisionProtocol,
    onSelect: (DecisionProtocol) -> Unit,
    modifier: Modifier = Modifier,
) {
    val protocols = DecisionProtocol.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        protocols.forEachIndexed { index, protocol ->
            SegmentedButton(
                selected = protocol == selected,
                onClick = { onSelect(protocol) },
                shape = SegmentedButtonDefaults.itemShape(index, protocols.size),
            ) {
                Text(stringResource(protocol.labelRes))
            }
        }
    }
}

private val DecisionProtocol.labelRes: Int
    get() = when (this) {
        DecisionProtocol.SystemOne -> R.string.protocol_system_one
        DecisionProtocol.OpenAiDecisions -> R.string.protocol_openai_decisions
    }

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelField(
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    onOpen: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val showMenu = expanded && options.isNotEmpty()
    ExposedDropdownMenuBox(
        expanded = showMenu,
        onExpandedChange = {
            expanded = it
            if (it) onOpen()
        },
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.label_model)) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showMenu) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            shape = RoundedCornerShape(12.dp),
        )
        ExposedDropdownMenu(expanded = showMenu, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SecretField(value: String, onValueChange: (String) -> Unit, label: Int) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(stringResource(if (visible) R.string.action_hide else R.string.action_show))
            }
        },
        shape = RoundedCornerShape(12.dp),
    )
}
