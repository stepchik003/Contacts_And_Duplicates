package com.example.contactsapp.data.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.contactsapp.IContactsService
import com.example.contactsapp.IDuplicateCallback
import com.example.contactsapp.domain.model.DeletionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject


class ContactsDuplicateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun deleteDuplicatesFlow(): Flow<DeletionResult> = callbackFlow {

        var contactsService: IContactsService? = null

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                contactsService = IContactsService.Stub.asInterface(service)

                val callback = object : IDuplicateCallback.Stub() {
                    override fun onResult(statusCode: Int) {
                        val result = when (statusCode) {
                            ContactsDuplicateService.STATUS_SUCCESS -> DeletionResult.SUCCESS
                            ContactsDuplicateService.STATUS_NOT_FOUND -> DeletionResult.NOT_FOUND
                            else -> DeletionResult.ERROR
                        }
                        trySend(result)
                        close()
                    }
                }

                contactsService?.deleteDuplicates(callback)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                contactsService = null
            }
        }

        val intent = Intent(context, ContactsDuplicateService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        awaitClose {
            context.unbindService(connection)
        }
    }
}