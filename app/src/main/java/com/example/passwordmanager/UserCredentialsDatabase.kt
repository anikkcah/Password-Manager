package com.example.passwordmanager

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.passwordmanager.db.UserCredentials
import com.example.passwordmanager.db.dao.UserCredentialsDao


@Database(entities = [UserCredentials::class], version = 1, exportSchema = false)
abstract class UserCredentialsDatabase : RoomDatabase() {
    abstract fun userCredentialsDao(): UserCredentialsDao


}


