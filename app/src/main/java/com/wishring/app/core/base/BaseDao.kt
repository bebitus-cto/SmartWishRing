package com.wishring.app.core.base

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

/**
 * Base DAO interface providing common CRUD operations
 * All DAO interfaces should extend this interface
 * 
 * @param T Entity type
 */
interface BaseDao<T> {
    
    /**
     * Insert a single entity
     * @param entity Entity to insert
     * @return Row ID of the inserted entity
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T): Long
    
    /**
     * Insert multiple entities
     * @param entities List of entities to insert
     * @return List of row IDs of inserted entities
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<T>): List<Long>
    
    /**
     * Update a single entity
     * @param entity Entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun update(entity: T): Int
    
    /**
     * Update multiple entities
     * @param entities List of entities to update
     * @return Number of rows updated
     */
    @Update
    suspend fun updateAll(entities: List<T>): Int
    
    /**
     * Delete a single entity
     * @param entity Entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun delete(entity: T): Int
    
    /**
     * Delete multiple entities
     * @param entities List of entities to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun deleteAll(entities: List<T>): Int
}