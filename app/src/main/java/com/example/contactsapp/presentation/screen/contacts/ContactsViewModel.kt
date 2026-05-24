package com.example.contactsapp.presentation.screen.contacts


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactsapp.domain.model.Contact
import com.example.contactsapp.domain.usecases.GetContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.contactsapp.domain.model.DeletionResult
import com.example.contactsapp.domain.usecases.DeleteDuplicatesUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val deleteDuplicatesUseCase: DeleteDuplicatesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<ContactsState>(ContactsState.Loading)
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ContactsEvent>()
    val effect = _effect.asSharedFlow()

    init {
        loadContacts()
    }

    fun refresh() {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _state.value = ContactsState.Loading

            try {
                val contacts = getContactsUseCase()
                _state.value = ContactsState.Success(contacts.groupByInitial())
            } catch (e: Exception) {
                _state.value = ContactsState.Error("Ошибка загрузки контактов")
            }
        }
    }

    fun onContactClick(phoneNumber: String) {
        viewModelScope.launch {
            _effect.emit(ContactsEvent.CallContact(phoneNumber))
        }
    }

    fun onDeleteDuplicatesClicked() {
        val currentState = _state.value
        if (currentState is ContactsState.Success) {
            viewModelScope.launch {
                _state.value = currentState.copy(isDeletingDuplicates = true)

                deleteDuplicatesUseCase().collect { result ->
                    when (result) {
                        DeletionResult.SUCCESS -> {
                            _effect.emit(ContactsEvent.ShowMessage("Повторяющиеся контакты удалены успешно"))
                            loadContacts()
                        }
                        DeletionResult.NOT_FOUND -> {
                            _state.value = currentState.copy(isDeletingDuplicates = false)
                            _effect.emit(ContactsEvent.ShowMessage("Повторяющиеся контакты не найдены"))
                        }
                        DeletionResult.ERROR -> {
                            _state.value = currentState.copy(isDeletingDuplicates = false)
                            _effect.emit(ContactsEvent.ShowMessage("Произошла ошибка"))
                        }
                    }
                }
            }
        }
    }
}

private fun List<Contact>.groupByInitial(): Map<Char, List<Contact>> =
    groupBy { it.name.first().uppercaseChar() }
        .toSortedMap()