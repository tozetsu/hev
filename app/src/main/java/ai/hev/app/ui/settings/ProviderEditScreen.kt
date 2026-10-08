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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ai.hev.app.HevApp
import ai.hev.app.R
import ai.hev.app.data.local.prefs.ProviderStore
import ai.hev.app.domain.provider.ProviderConfig
import ai.hev.app.ui.navigation.Routes
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditScreen(
    providerId: String,
    onBack: () -> Unit,
) {
    val repo = (LocalContext.current.applicationContext as HevApp).container.repository
    val isNew = providerId == Routes.NEW_PROVIDER
    val existing = if (isNew) null else repo.providers.value.firstOrNull { it.id == providerId }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var endpoint by remember { mutableStateOf(existing?.baseUrl ?: ProviderStore.DEFAULT_BASE) }
    var apiKey by remember { mutableStateOf(existing?.apiKey ?: "") }
    var model by remember { mutableStateOf(existing?.model ?: "jev-latest") }
    var showKey by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val errName = stringResource(R.string.error_name_required)
    val errEndpoint = stringResource(R.string.error_endpoint_required)
    val errModel = stringResource(R.string.error_model_required)

    Scaffold(
        contentWindowInsets = WindowInsets.safeContent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNew) stringResource(R.string.provider_new)
                        else stringResource(R.string.provider_edit),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.cd_delete))
                        }
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
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
            item {
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text(stringResource(R.string.label_endpoint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
            item {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.label_api_key)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showKey = !showKey }) {
                            Text(
                                if (showKey) stringResource(R.string.action_hide)
                                else stringResource(R.string.action_show),
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                )
            }
            item {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(stringResource(R.string.label_model)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
            item {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        when {
                            name.isBlank() -> error = errName
                            endpoint.isBlank() -> error = errEndpoint
                            model.isBlank() -> error = errModel
                            else -> {
                                val cfg = ProviderConfig(
                                    id = existing?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    baseUrl = endpoint.trim(),
                                    apiKey = apiKey.trim(),
                                    model = model.trim(),
                                )
                                repo.upsertProvider(cfg)
                                if (isNew || repo.activeProvider() == null) {
                                    repo.setActiveProvider(cfg.id)
                                }
                                onBack()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_provider_title)) },
            text = { Text(stringResource(R.string.delete_provider_message, existing.name)) },
            confirmButton = {
                TextButton(onClick = {
                    repo.deleteProvider(existing.id)
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
