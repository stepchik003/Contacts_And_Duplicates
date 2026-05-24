package com.example.contactsapp;

import com.example.contactsapp.IDuplicateCallback;

interface IContactsService {
    void deleteDuplicates(IDuplicateCallback callback);
}