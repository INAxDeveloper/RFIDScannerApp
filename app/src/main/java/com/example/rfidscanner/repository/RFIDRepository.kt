package com.example.rfidscanner.repository

import androidx.lifecycle.LiveData
import com.example.rfidscanner.data.RFIDTagDao
import com.example.rfidscanner.model.RFIDTag
import kotlinx.coroutines.flow.Flow
import java.util.Date

class RFIDRepository(private val tagDao: RFIDTagDao) {

    val allTags: LiveData<List<RFIDTag>> = tagDao.getAllTags()

    suspend fun insertOrUpdateTag(tag: RFIDTag) {
        val existingTag = tagDao.getTagByEpc(tag.epc)
        
        if (existingTag != null) {
            // Update existing tag
            existingTag.seenCount++
            existingTag.lastSeen = Date()
            existingTag.rssi = tag.rssi ?: existingTag.rssi
            tagDao.update(existingTag)
        } else {
            // Insert new tag
            tagDao.insert(tag)
        }
    }

    suspend fun insertOrUpdateTags(tags: List<RFIDTag>) {
        tags.forEach { tag ->
            insertOrUpdateTag(tag)
        }
    }

    suspend fun deleteAllTags() {
        tagDao.deleteAllTags()
    }


    suspend fun getTagCount(): Int {
        return tagDao.getTagCount()
    }

    suspend fun getTagByEpc(epc: String): RFIDTag? {
        return tagDao.getTagByEpc(epc)
    }
}