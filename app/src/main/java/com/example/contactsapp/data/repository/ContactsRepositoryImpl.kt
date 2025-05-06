package com.example.contactsapp.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.example.contactsapp.domain.model.Contact
import com.example.contactsapp.domain.repository.ContactsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ContactsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ContactsRepository {

    override suspend fun getContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            PROJECTION, null, null, "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

            while (cursor.moveToNext()) {
                contacts.add(
                    Contact(
                        id = cursor.getLong(idIndex),
                        name = parseName(cursor.getString(nameIndex)),
                        surname = parseSurname(cursor.getString(nameIndex)),
                        phoneNumber = cursor.getString(phoneIndex).orEmpty(),
                        photoUri = cursor.getString(photoIndex)
                    )
                )
            }
        }
        contacts
    }

    private fun parseName(fullName: String?): String = fullName?.split(" ")?.firstOrNull() ?: ""
    private fun parseSurname(fullName: String?): String = fullName?.split(" ")?.getOrNull(1) ?: ""

    companion object {
        private val PROJECTION = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.Contacts.PHOTO_URI
        )
    }
}