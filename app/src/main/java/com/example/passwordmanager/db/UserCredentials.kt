package com.example.passwordmanager.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "UserCred")
data class UserCredentials(
    @PrimaryKey @ColumnInfo(name="sitename") val sitename: String,
    @ColumnInfo(name = "username")val username: String,
    @ColumnInfo(name = "password")val password: String
)