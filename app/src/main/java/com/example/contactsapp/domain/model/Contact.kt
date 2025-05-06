package com.example.contactsapp.domain.model

data class Contact(
    val id: Long,
    val name: String,
    val surname: String,
    val phoneNumber: String,
    val photoUri: String?
) {
    val fullName: String get() = listOf(name, surname).filter { it.isNotBlank() }.joinToString(" ")
}
