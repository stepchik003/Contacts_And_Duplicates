package com.example.contactsapp.domain.repository

import com.example.contactsapp.domain.model.Contact

interface ContactsRepository {
    suspend fun getContacts(): List<Contact>
}