package com.example.contactsapp.data.service

import android.app.Service
import android.content.ContentProviderOperation
import android.content.Intent
import android.os.IBinder
import android.provider.ContactsContract
import com.example.contactsapp.IContactsService
import com.example.contactsapp.IDuplicateCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ContactsDuplicateService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val STATUS_SUCCESS = 0
        const val STATUS_ERROR = 1
        const val STATUS_NOT_FOUND = 2
    }

    private val binder = object : IContactsService.Stub() {
        override fun deleteDuplicates(callback: IDuplicateCallback?) {
            serviceScope.launch {
                try {
                    val duplicatesCount = findAndDeleteDuplicates()
                    if (duplicatesCount > 0) {
                        callback?.onResult(STATUS_SUCCESS)
                    } else {
                        callback?.onResult(STATUS_NOT_FOUND)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback?.onResult(STATUS_ERROR)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun findAndDeleteDuplicates(): Int {
        val contactsMap = mutableMapOf<String, MutableSet<Long>>()
        val projection = arrayOf(
            ContactsContract.Data.RAW_CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf("com.google")

        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: ""
                val phone = cursor.getString(phoneIndex)?.replace(Regex("\\D"), "") ?: ""

                val uniqueKey = "${name}_${phone}"

                if (!contactsMap.containsKey(uniqueKey)) {
                    contactsMap[uniqueKey] = mutableSetOf()
                }
                contactsMap[uniqueKey]?.add(id)
            }
        }

        val duplicateIdsToDelete = mutableListOf<Long>()
        for ((_, ids) in contactsMap) {
            if (ids.size > 1) {
                duplicateIdsToDelete.addAll(ids.drop(1))
            }
        }

        if (duplicateIdsToDelete.isEmpty()) {
            return 0
        }

        val deleteUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon()
            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
            .build()

        val operations = ArrayList<ContentProviderOperation>()
        for (duplicateId in duplicateIdsToDelete) {
            operations.add(
                ContentProviderOperation.newDelete(deleteUri)
                    .withSelection(
                        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                        arrayOf(duplicateId.toString())
                    )
                    .build()
            )
        }

        contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        return duplicateIdsToDelete.size
    }
}