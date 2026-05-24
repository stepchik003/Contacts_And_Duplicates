package com.example.contactsapp.domain.usecases

import com.example.contactsapp.data.service.ContactsDuplicateManager
import com.example.contactsapp.domain.model.DeletionResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeleteDuplicatesUseCase @Inject constructor(
    private val duplicateManager: ContactsDuplicateManager
) {

    operator fun invoke(): Flow<DeletionResult> {
        return duplicateManager.deleteDuplicatesFlow()
    }
}