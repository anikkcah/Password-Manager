package com.example.passwordmanager.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.passwordmanager.db.UserCredentials
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCredentialsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userCredentials: UserCredentials) : Long

    @Delete
    suspend fun delete(userCredentials: UserCredentials) : Int

    @Update
    suspend fun update(userCredentials: UserCredentials) : Int

    @Query("SELECT * FROM UserCred")
    fun getAllCredentials(): Flow<List<UserCredentials>>

    // Add more queries for filtering or searching passwords
}