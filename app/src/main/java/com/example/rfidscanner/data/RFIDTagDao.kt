package com.example.rfidscanner.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.rfidscanner.model.RFIDTag

@Dao
interface RFIDTagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: RFIDTag)

    @Update
    suspend fun update(tag: RFIDTag)

    @Query("SELECT * FROM rfid_tags ORDER BY lastSeen DESC")
    fun getAllTags(): LiveData<List<RFIDTag>>

    @Query("SELECT * FROM rfid_tags WHERE epc = :epc")
    suspend fun getTagByEpc(epc: String): RFIDTag?

    @Query("DELETE FROM rfid_tags")
    suspend fun deleteAllTags()

    @Query("SELECT COUNT(*) FROM rfid_tags")
    suspend fun getTagCount(): Int
}