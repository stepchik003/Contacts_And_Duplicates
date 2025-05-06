package com.example.contactsapp.presentation.screen.contacts

import com.example.contactsapp.domain.model.Contact

data class ContactsState(
    val contacts: Map<Char, List<Contact>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val shouldShowContent: Boolean
        get() = !isLoading && error == null
}