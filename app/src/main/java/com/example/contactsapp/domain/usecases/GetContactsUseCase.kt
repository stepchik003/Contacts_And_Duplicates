package com.example.contactsapp.domain.usecases

import com.example.contactsapp.domain.model.Contact
import com.example.contactsapp.domain.repository.ContactsRepository
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(): List<Contact> = repository.getContacts()
}