package com.example.contactsapp.presentation.screen.contacts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactsapp.domain.model.Contact
import com.example.contactsapp.domain.usecases.GetContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ContactsState())
    val state: StateFlow<ContactsState> = _state.asStateFlow()

    init {
        loadContacts()
    }

    fun refresh() {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val contacts = getContactsUseCase()
                _state.update {
                    it.copy(
                        contacts = contacts.groupByInitial(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load contacts"
                    )
                }
            }
        }
    }

    fun onContactClick(phoneNumber: String, context: Context) {
        viewModelScope.launch {
            try {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val callIntent = Intent(Intent.ACTION_CALL).apply {
                        data = "tel:$phoneNumber".toUri()
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (callIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(callIntent)
                    } else {
                        _state.update { it.copy(error = "No app available to handle calls") }
                    }
                } else {
                    _state.update { it.copy(error = "Call permission not granted") }
                }
            } catch (e: SecurityException) {
                _state.update { it.copy(error = "Call permission denied: ${e.message}") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to make call: ${e.message}") }
            }
        }
    }
}

private fun List<Contact>.groupByInitial(): Map<Char, List<Contact>> =
    groupBy { it.name.first().uppercaseChar() }
        .toSortedMap()