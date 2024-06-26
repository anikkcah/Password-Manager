package com.example.passwordmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.passwordmanager.db.repository.UserCredentialsRepository

class CredentialsViewModelFactory(private val repository: UserCredentialsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CredentialsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CredentialsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}