package com.example.contactsapp.presentation.screen.contacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.contactsapp.domain.model.Contact
import com.example.contactsapp.presentation.components.ContactItem
import com.example.contactsapp.presentation.components.LetterHeader
import com.example.contactsapp.presentation.components.LoadingIndicator
import androidx.core.net.toUri

private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.WRITE_CONTACTS
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            viewModel.refresh()
            showPermissionDialog = false
        } else {
            showPermissionDialog = true
        }
    }

    LaunchedEffect(Unit) {
        val hasPermissions = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasPermissions) {
            viewModel.refresh()
        } else {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    if (showPermissionDialog) {
        PermissionDeniedDialog(
            onDismiss = { showPermissionDialog = false },
            onRequestAgain = {
                showPermissionDialog = false
                permissionLauncher.launch(REQUIRED_PERMISSIONS)
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ContactsEvent.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ContactsEvent.CallContact -> {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = "tel:${effect.phoneNumber}".toUri()
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Нет приложения для звонков", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                actions = {
                    if (state is ContactsState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state is ContactsState.Success) {
                val successState = state as ContactsState.Success
                Button(
                    onClick = { viewModel.onDeleteDuplicatesClicked() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !successState.isDeletingDuplicates
                ) {
                    if (successState.isDeletingDuplicates) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Удалить одинаковые контакты")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val currentState = state) {
                is ContactsState.Loading -> {
                    LoadingIndicator()
                }
                is ContactsState.Error -> {
                    ErrorView(
                        error = currentState.message,
                        onRetry = {
                            viewModel.refresh()
                        }
                    )
                }
                is ContactsState.Success -> {
                    ContactList(
                        contacts = currentState.contacts,
                        onContactClick = { phone ->
                            viewModel.onContactClick(phone)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionDeniedDialog(
    onDismiss: () -> Unit,
    onRequestAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Необходимы разрешения") },
        text = { Text("Для работы всех функций приложения предоставьте разрешения на чтение и изменение контактов.") },
        confirmButton = {
            Button(onClick = onRequestAgain) {
                Text("Запросить повторно")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun ContactList(
    contacts: Map<Char, List<Contact>>,
    onContactClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        contacts.forEach { (initial, contactsInGroup) ->
            item(key = "header_$initial") {
                LetterHeader(initial.toString())
            }
            items(
                items = contactsInGroup,
                key = { it.id }
            ) { contact ->
                ContactItem(
                    contact = contact,
                    onClick = { onContactClick(contact.phoneNumber) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Повторить")
        }
    }
}