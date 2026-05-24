package com.example.contactsapp.presentation.screen.contacts

import com.example.contactsapp.domain.model.Contact

sealed interface ContactsState {
    data object Loading : ContactsState

    data class Success(
        val contacts: Map<Char, List<Contact>>,
        val isDeletingDuplicates: Boolean = false
    ) : ContactsState

    data class Error(val message: String) : ContactsState
}

sealed interface ContactsEvent {
    data class ShowMessage(val message: String) : ContactsEvent
    data class CallContact(val phoneNumber: String) : ContactsEvent
}