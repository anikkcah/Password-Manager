package com.example.passwordmanager.db.repository

import com.example.passwordmanager.db.UserCredentials
import com.example.passwordmanager.db.dao.UserCredentialsDao
import kotlinx.coroutines.flow.Flow

class UserCredentialsRepository(private val userCredentialsDao: UserCredentialsDao) {
    suspend fun insertCredentials(userCredentials: UserCredentials) =
        userCredentialsDao.insert(userCredentials)

    suspend fun deleteCredentials(userCredentials: UserCredentials) =
        userCredentialsDao.delete(userCredentials)

    suspend fun updateCredentials(userCredentials: UserCredentials) =
        userCredentialsDao.update(userCredentials)

    fun getAllCredentials() :Flow<List<UserCredentials>> =
        userCredentialsDao.getAllCredentials()
}