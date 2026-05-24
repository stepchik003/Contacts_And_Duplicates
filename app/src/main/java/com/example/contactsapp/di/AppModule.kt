package com.example.contactsapp.di

import android.content.ContentResolver
import android.content.Context
import coil.ImageLoader
import com.example.contactsapp.data.repository.ContactsRepositoryImpl
import com.example.contactsapp.data.service.ContactsDuplicateManager
import com.example.contactsapp.domain.repository.ContactsRepository
import com.example.contactsapp.domain.usecases.DeleteDuplicatesUseCase
import com.example.contactsapp.domain.usecases.GetContactsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideContactsRepository(
        @ApplicationContext context: Context
    ): ContactsRepository {
        return ContactsRepositoryImpl(context)
    }

    @Provides
    fun provideGetContactsUseCase(
        repository: ContactsRepository
    ): GetContactsUseCase {
        return GetContactsUseCase(repository)
    }

    @Provides
    fun provideImageLoader(
        @ApplicationContext context: Context
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }

    @Provides
    fun provideContentResolver(
        @ApplicationContext context: Context
    ): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun provideContactsDuplicateManager(
        @ApplicationContext context: Context
    ): ContactsDuplicateManager {
        return ContactsDuplicateManager(context)
    }

    @Provides
    fun provideDeleteDuplicatesUseCase(
        manager: ContactsDuplicateManager
    ): DeleteDuplicatesUseCase {
        return DeleteDuplicatesUseCase(manager)
    }
}