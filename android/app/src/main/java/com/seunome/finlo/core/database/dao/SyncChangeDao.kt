package com.seunome.finlo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.seunome.finlo.core.database.entity.SyncChangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncChangeDao {
    @Query("SELECT * FROM sync_queue ORDER BY clientUpdated ASC LIMIT :limit")
    suspend fun getPendingChanges(limit: Int = 100): List<SyncChangeEntity>

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getPendingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(change: SyncChangeEntity)

    @Query("DELETE FROM sync_queue WHERE id IN (:ids)")
    suspend fun removeChanges(ids: List<String>)

    @Query("DELETE FROM sync_queue")
    suspend fun clear()
}
