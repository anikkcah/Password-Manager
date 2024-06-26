package com.example.passwordmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordmanager.db.UserCredentials
import com.example.passwordmanager.db.repository.UserCredentialsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CredentialsViewModel(private val repository: UserCredentialsRepository): ViewModel() {

    val allCredentials : Flow<List<UserCredentials>> =
        repository.getAllCredentials()

    fun addCredentials(sitename: String, username: String, password: String){

        viewModelScope.launch {
            repository.insertCredentials(UserCredentials(sitename = sitename, username = username, password = password))
        }
    }

    fun deleteCredentials(userCredentials: UserCredentials){
        viewModelScope.launch{
            repository.deleteCredentials(userCredentials)
        }
    }

    fun updateCredentials(sitename: String, username: String, password: String) {
        viewModelScope.launch{
            repository.updateCredentials(UserCredentials(sitename = sitename, username = username, password = password))
        }

    }
}