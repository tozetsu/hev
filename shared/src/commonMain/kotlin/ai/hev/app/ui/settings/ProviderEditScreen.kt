package ai.hev.app.ui.settings

import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.resources.Res
import ai.hev.app.resources.action_cancel
import ai.hev.app.resources.action_delete
import ai.hev.app.resources.action_hide
import ai.hev.app.resources.action_save
import ai.hev.app.resources.action_show
import ai.hev.app.resources.cd_delete
import ai.hev.app.resources.delete_provider_message
import ai.hev.app.resources.delete_provider_title
import ai.hev.app.resources.label_api_key
import ai.hev.app.resources.label_endpoint
import ai.hev.app.resources.label_model
import ai.hev.app.resources.label_name
import ai.hev.app.resources.protocol_openai_decisions
import ai.hev.app.resources.protocol_system_one
import ai.hev.app.resources.provider_edit
import ai.hev.app.resources.provider_new
import ai.hev.app.ui.common.LocalAppGraph
import ai.hev.app.ui.common.text
import ai.hev.app.ui.components.ActionIcon
import ai.hev.app.ui.components.BackButton
import ai.hev.app.ui.components.HevIcons
import ai.hev.app.ui.components.ScrollColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditScreen(
    providerId: String?,
    onBack: () -> Unit,
) {
    val repo = LocalAppGraph.current.repository
    val vm = viewModel { ProviderEditViewModel(repo, providerId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeContent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) Res.string.provider_new else Res.string.provider_edit)) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (!state.isNew) {
                        ActionIcon(
                            HevIcons.Delete,
                            stringResource(Res.string.cd_delete),
                            onClick = { confirmDelete = true },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        ScrollColumn(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Field(state.name, vm::setName, Res.string.label_name)
            }
            item {
                ProtocolSelector(
                    selected = state.protocol,
                    onSelect = vm::setProtocol,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item {
                Field(state.endpoint, vm::setEndpoint, Res.string.label_endpoint, keyboardType = KeyboardType.Uri)
            }
            item {
                SecretField(value = state.apiKey, onValueChange = vm::setApiKey, label = Res.string.label_api_key)
            }
            item {
                ModelField(
                    value = state.model,
                    options = state.fetchedModels,
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
                    Text(stringResource(Res.string.action_save))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.delete_provider_title)) },
            text = { Text(stringResource(Res.string.delete_provider_message, state.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    if (vm.delete()) onBack()
                }) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
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

private val DecisionProtocol.labelRes: StringResource
    get() = when (this) {
        DecisionProtocol.SystemOne -> Res.string.protocol_system_one
        DecisionProtocol.OpenAiDecisions -> Res.string.protocol_openai_decisions
    }

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource,
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
            label = { Text(stringResource(Res.string.label_model)) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showMenu) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
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
private fun SecretField(value: String, onValueChange: (String) -> Unit, label: StringResource) {
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
                Text(stringResource(if (visible) Res.string.action_hide else Res.string.action_show))
            }
        },
        shape = RoundedCornerShape(12.dp),
    )
}
